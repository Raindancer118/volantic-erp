package de.volantic.erp.changeset.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.audit.AuditTrail;
import de.volantic.erp.changeset.application.ChangeSetExceptions.ChangeSetAccessDeniedException;
import de.volantic.erp.changeset.application.ChangeSetExceptions.ChangeSetNotFoundException;
import de.volantic.erp.changeset.application.ChangeSetExceptions.FieldNotEditableException;
import de.volantic.erp.changeset.application.ChangeSetExceptions.FieldNotFilterableException;
import de.volantic.erp.changeset.application.port.out.ChangeSetStore;
import de.volantic.erp.changeset.domain.model.ChangeSet;
import de.volantic.erp.changeset.domain.model.ChangeSetId;
import de.volantic.erp.changeset.domain.model.ChangeSetMode;
import de.volantic.erp.changeset.domain.model.ChangeSetStatus;
import de.volantic.erp.changeset.domain.model.RecordedOperation;
import de.volantic.erp.workflow.Approvals;
import de.volantic.erp.core.entitylink.EntityRef;
import de.volantic.erp.core.revision.BulkEditHandler;
import de.volantic.erp.core.revision.ChangeOperation;
import de.volantic.erp.core.revision.DocumentPostingHandler;
import de.volantic.erp.core.revision.LifecycleResourceHandler;
import de.volantic.erp.core.revision.ReversibleResourceHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use cases behind mass edits, the Probemodus and the Rollback Engine (ADR-0006). Enforcement is at this
 * service boundary (ADR-0004) via {@code @PreAuthorize}. Operations are recorded in a {@link ChangeSet}
 * and driven through the {@code core.revision} handlers collected by {@link ResourceHandlers}; every real
 * write also goes to the tamper-evident {@link AuditTrail}. Mass edits currently cover field updates.
 */
@Service
public class ChangeSetService {

    private static final TypeReference<Map<String, String>> FIELD_MAP = new TypeReference<>() {
    };

    /** Subject type used for the four-eyes approval of a change-set session (ADR-0006 §7). */
    public static final String APPROVAL_SUBJECT_TYPE = "changeset.session";

    private final ChangeSetStore store;
    private final ResourceHandlers handlers;
    private final AuditTrail audit;
    private final ObjectMapper json;
    private final Approvals approvals;

    ChangeSetService(ChangeSetStore store, ResourceHandlers handlers, AuditTrail audit,
                     ObjectMapper json, Approvals approvals) {
        this.store = store;
        this.handlers = handlers;
        this.audit = audit;
        this.json = json;
        this.approvals = approvals;
    }

    /** Starts a LIVE session: changes take effect immediately and can be taken back via {@link #revert}. */
    @Transactional
    @PreAuthorize("hasPermission(null, 'changeset.bulk:execute')")
    public ChangeSetId beginLive() {
        return begin(ChangeSetMode.LIVE);
    }

    /** Activates the Probemodus: a DEFERRED session whose changes are only applied on {@link #commit}. */
    @Transactional
    @PreAuthorize("hasPermission(null, 'changeset.probemodus:activate')")
    public ChangeSetId beginProbemodus() {
        return begin(ChangeSetMode.DEFERRED);
    }

    private ChangeSetId begin(ChangeSetMode mode) {
        ChangeSet session = ChangeSet.open(currentActor(), mode);
        store.save(session);
        return session.id();
    }

    /** Dry-run of a mass edit: reports the current state and any problem per resource, without mutating. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'changeset.bulk:execute')")
    public BulkPreview preview(BulkChange change) {
        BulkEditHandler bulk = handlers.bulkFor(change.resourceType());
        ReversibleResourceHandler reversible = handlers.reversibleFor(change.resourceType());
        List<String> uneditable = uneditableFields(change, bulk);
        List<UUID> targets = resolveTargets(change, bulk);

        List<BulkPreview.Row> rows = new ArrayList<>();
        for (var id : targets) {
            String before = reversible.capture(id);
            if (!uneditable.isEmpty()) {
                rows.add(new BulkPreview.Row(id, false, before, "fields not editable: " + uneditable));
            } else if (before == null) {
                rows.add(new BulkPreview.Row(id, false, null, "resource does not exist"));
            } else {
                rows.add(new BulkPreview.Row(id, true, before, null));
            }
        }
        return new BulkPreview(change.resourceType(), rows);
    }

    /** Applies (LIVE) or buffers (Probemodus) a mass edit within the given session. */
    @Transactional
    @PreAuthorize("hasPermission(null, 'changeset.bulk:execute')")
    public void apply(ChangeSetId session, BulkChange change) {
        ChangeSet changeSet = loadOwned(session);
        BulkEditHandler bulk = handlers.bulkFor(change.resourceType());
        ReversibleResourceHandler reversible = handlers.reversibleFor(change.resourceType());
        rejectUneditableFields(change, bulk);
        List<UUID> targets = resolveTargets(change, bulk);

        String payload = serialize(change.fieldChanges());
        boolean deferred = changeSet.mode() == ChangeSetMode.DEFERRED;
        for (var id : targets) {
            EntityRef target = EntityRef.of(change.resourceType(), id);
            if (deferred) {
                changeSet.record(new RecordedOperation(target, ChangeOperation.UPDATE, null, payload, now()));
            } else {
                String before = reversible.capture(id);
                bulk.applyChange(id, change.fieldChanges());
                changeSet.record(new RecordedOperation(target, ChangeOperation.UPDATE, before, payload, now()));
                audit.record("changeset.bulk-applied", change.resourceType(), id, payload);
            }
        }
        store.save(changeSet);
    }

    /**
     * Mass-creates resources within a LIVE session (ADR-0006 §2). Each created resource is recorded as a
     * CREATE operation so the Rollback Engine can take it back by deleting it. Not available in Probemodus
     * (the ids do not exist until applied).
     */
    @Transactional
    @PreAuthorize("hasPermission(null, 'changeset.bulk:execute')")
    public List<UUID> createBulk(ChangeSetId session, BulkCreate request) {
        ChangeSet changeSet = loadOwned(session);
        requireLive(changeSet, "create");
        LifecycleResourceHandler lifecycle = handlers.lifecycleFor(request.resourceType());

        List<UUID> created = new ArrayList<>();
        for (Map<String, String> record : request.records()) {
            UUID id = lifecycle.create(record);
            String payload = serialize(record);
            changeSet.record(new RecordedOperation(
                    EntityRef.of(request.resourceType(), id), ChangeOperation.CREATE, null, payload, now()));
            audit.record("changeset.bulk-created", request.resourceType(), id, payload);
            created.add(id);
        }
        store.save(changeSet);
        return created;
    }

    /**
     * Mass-deletes the selected resources within a LIVE session (ADR-0006 §2). Each deletion captures a
     * full snapshot first and is recorded as a DELETE operation, so the Rollback Engine can re-create the
     * resource with its original id (forward-only — the audit history is never erased). Not available in
     * Probemodus.
     */
    @Transactional
    @PreAuthorize("hasPermission(null, 'changeset.bulk:execute')")
    public void deleteBulk(ChangeSetId session, BulkDelete request) {
        ChangeSet changeSet = loadOwned(session);
        requireLive(changeSet, "delete");
        LifecycleResourceHandler lifecycle = handlers.lifecycleFor(request.resourceType());

        for (UUID id : resolveDeleteTargets(request)) {
            String snapshot = lifecycle.snapshot(id);
            if (snapshot == null) {
                continue; // already gone — nothing to delete or to compensate
            }
            lifecycle.delete(id);
            changeSet.record(new RecordedOperation(
                    EntityRef.of(request.resourceType(), id), ChangeOperation.DELETE, snapshot, null, now()));
            audit.record("changeset.bulk-deleted", request.resourceType(), id, snapshot);
        }
        store.save(changeSet);
    }

    /**
     * Mass-posts existing draft documents (Belege) within a LIVE session (ADR-0006 §2): each is posted
     * through the module's domain service (drawing its gap-free GoBD number) and recorded as a POST
     * operation, so the Rollback Engine can take the whole batch back with stornos. Not available in
     * Probemodus (posting draws real numbers and cannot be buffered).
     */
    @Transactional
    @PreAuthorize("hasPermission(null, 'changeset.bulk:execute')")
    public void postDocuments(ChangeSetId session, BulkPostDocuments request) {
        ChangeSet changeSet = loadOwned(session);
        requireLive(changeSet, "post documents");
        DocumentPostingHandler posting = handlers.postingFor(request.resourceType());

        for (UUID id : request.ids()) {
            posting.post(id);
            changeSet.record(new RecordedOperation(
                    EntityRef.of(request.resourceType(), id), ChangeOperation.POST, null, null, now()));
            audit.record("changeset.document-posted", request.resourceType(), id, null);
        }
        store.save(changeSet);
    }

    /** Closes a session: a Probemodus session applies its buffered operations ("Übertragen"). */
    @Transactional
    @PreAuthorize("hasPermission(null, 'changeset.bulk:execute')")
    public void commit(ChangeSetId session) {
        ChangeSet changeSet = loadOwned(session);
        if (changeSet.mode() == ChangeSetMode.DEFERRED) {
            applyBufferedOperations(changeSet);
        }
        changeSet.commit();
        store.save(changeSet);
    }

    /**
     * Submits a Probemodus session for four-eyes approval (ADR-0006 §7): it can no longer be committed
     * directly — a reviewer must approve it, which then applies it automatically (see
     * {@link #applyApprovalOutcome}). Returns the workflow approval instance id. The approval is started
     * before the session is moved to {@code AWAITING_APPROVAL} so that a failure leaves the session open.
     */
    @Transactional
    @PreAuthorize("hasPermission(null, 'changeset.bulk:execute')")
    public String requestApproval(ChangeSetId session) {
        ChangeSet changeSet = loadOwned(session);
        if (changeSet.mode() != ChangeSetMode.DEFERRED || changeSet.status() != ChangeSetStatus.OPEN) {
            throw new IllegalStateException("only an open Probemodus session can be submitted for approval");
        }
        String instanceId = approvals.requestApproval(
                APPROVAL_SUBJECT_TYPE, session.value().toString(), currentActor());
        changeSet.submitForApproval();
        store.save(changeSet);
        return instanceId;
    }

    /**
     * Applies a reviewer's decision to a session awaiting approval (ADR-0006 §7): approve → its buffered
     * operations are applied and it is committed; reject → it is discarded. Triggered by the workflow
     * {@code ApprovalDecided} event, running as the original requester (see {@code ChangeSetApprovalListener}),
     * so authorization and ownership resolve against that actor. Idempotent: a session that is no longer
     * awaiting approval is left untouched.
     */
    @Transactional
    @PreAuthorize("hasPermission(null, 'changeset.bulk:execute')")
    public void applyApprovalOutcome(ChangeSetId session, boolean approved) {
        ChangeSet changeSet = loadOwned(session);
        if (changeSet.status() != ChangeSetStatus.AWAITING_APPROVAL) {
            return;
        }
        if (approved) {
            applyBufferedOperations(changeSet);
            changeSet.approve();
        } else {
            changeSet.rejectApproval();
        }
        store.save(changeSet);
    }

    /**
     * Applies a Probemodus session's buffered operations, capturing each resource's before-state as the
     * change is written (it was null while buffered). The enriched operations let the Rollback Engine
     * compensate the now-committed session later.
     */
    private void applyBufferedOperations(ChangeSet changeSet) {
        List<RecordedOperation> captured = new ArrayList<>();
        for (RecordedOperation op : changeSet.operations()) {
            BulkEditHandler bulk = handlers.bulkFor(op.target().type());
            ReversibleResourceHandler reversible = handlers.reversibleFor(op.target().type());
            String before = reversible.capture(op.target().id());
            bulk.applyChange(op.target().id(), deserialize(op.payload()));
            audit.record("changeset.committed", op.target().type(), op.target().id(), op.payload());
            captured.add(op.withBeforeState(before));
        }
        changeSet.replaceWithCaptured(captured);
    }

    /** Throws away a Probemodus session before commit — nothing was applied. */
    @Transactional
    @PreAuthorize("hasPermission(null, 'changeset.bulk:execute')")
    public void discard(ChangeSetId session) {
        ChangeSet changeSet = loadOwned(session);
        changeSet.discard();
        store.save(changeSet);
    }

    /** Rollback Engine: takes a LIVE/committed session back via forward-only compensation. */
    @Transactional
    @PreAuthorize("hasPermission(null, 'changeset.rollback:revert')")
    public void revert(ChangeSetId session) {
        ChangeSet changeSet = loadOwned(session);
        for (RecordedOperation op : changeSet.operationsForReversal()) {
            compensate(op);
            audit.record("changeset.reverted", op.target().type(), op.target().id(), op.beforeState());
        }
        changeSet.revert();
        store.save(changeSet);
    }

    /** Loads one of the current actor's sessions for inspection (status, mode, recorded operations). */
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'changeset.bulk:execute')")
    public ChangeSet getSession(ChangeSetId session) {
        return loadOwned(session);
    }

    /** Lists the current actor's sessions (newest first) — the basis for a session overview / Rollback UI. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'changeset.bulk:execute')")
    public Page<ChangeSet> listSessions(Pageable pageable) {
        return store.findByActor(currentActor(), pageable);
    }

    private ChangeSet load(ChangeSetId session) {
        return store.findById(session).orElseThrow(() -> new ChangeSetNotFoundException(session));
    }

    /**
     * Loads a session and verifies the current actor owns it. The {@code changeset.bulk:execute}
     * permission only grants the ability to run bulk edits at all — it must not let one user commit,
     * discard or revert another user's in-progress session, so ownership is checked here in addition to
     * the coarse permission.
     */
    private ChangeSet loadOwned(ChangeSetId session) {
        ChangeSet changeSet = load(session);
        if (!changeSet.actor().equals(currentActor())) {
            throw new ChangeSetAccessDeniedException(session);
        }
        return changeSet;
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Resolves the resources a change targets: the explicit id list, or — for a filtered mass edit — the
     * ids the resource's handler matches for the given equality filter (ADR-0006 §5). Filter fields are
     * validated against the handler's {@link BulkEditHandler#filterableFields()} first.
     */
    /** Forward-only compensation of one recorded operation, dispatched by its kind (ADR-0006 §2). */
    private void compensate(RecordedOperation op) {
        String type = op.target().type();
        UUID id = op.target().id();
        switch (op.operation()) {
            case UPDATE -> handlers.reversibleFor(type).compensate(ChangeOperation.UPDATE, id, op.beforeState());
            case DELETE -> handlers.lifecycleFor(type).recreate(id, op.beforeState()); // re-create from snapshot
            case CREATE -> handlers.lifecycleFor(type).delete(id);                     // remove what was created
            case POST -> handlers.postingFor(type).storno(id);                         // cancel posted Beleg via storno
        }
    }

    private static void requireLive(ChangeSet changeSet, String operation) {
        if (changeSet.mode() != ChangeSetMode.LIVE) {
            throw new IllegalStateException(
                    "bulk " + operation + " is only available in a LIVE session, not the Probemodus");
        }
    }

    private List<UUID> resolveDeleteTargets(BulkDelete request) {
        if (!request.isFiltered()) {
            return request.ids();
        }
        BulkEditHandler selector = handlers.bulkFor(request.resourceType());
        request.filter().keySet().stream()
                .filter(field -> !selector.filterableFields().contains(field))
                .findFirst()
                .ifPresent(field -> {
                    throw new FieldNotFilterableException(request.resourceType(), field);
                });
        return selector.selectIds(request.filter());
    }

    private static List<UUID> resolveTargets(BulkChange change, BulkEditHandler handler) {
        if (!change.isFiltered()) {
            return change.ids();
        }
        change.filter().keySet().stream()
                .filter(field -> !handler.filterableFields().contains(field))
                .findFirst()
                .ifPresent(field -> {
                    throw new FieldNotFilterableException(change.resourceType(), field);
                });
        return handler.selectIds(change.filter());
    }

    private static List<String> uneditableFields(BulkChange change, BulkEditHandler handler) {
        return change.fieldChanges().keySet().stream()
                .filter(field -> !handler.editableFields().contains(field))
                .toList();
    }

    private static void rejectUneditableFields(BulkChange change, BulkEditHandler handler) {
        List<String> uneditable = uneditableFields(change, handler);
        if (!uneditable.isEmpty()) {
            throw new FieldNotEditableException(change.resourceType(), uneditable.getFirst());
        }
    }

    private String serialize(Map<String, String> fieldChanges) {
        try {
            return json.writeValueAsString(fieldChanges);
        } catch (JsonProcessingException e) {
            throw new ChangeSetExceptions.SerializationException("could not serialize field changes", e);
        }
    }

    private Map<String, String> deserialize(String payload) {
        try {
            return json.readValue(payload, FIELD_MAP);
        } catch (JsonProcessingException e) {
            throw new ChangeSetExceptions.SerializationException("could not deserialize buffered field changes", e);
        }
    }

    private static String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        return authentication.getName();
    }
}
