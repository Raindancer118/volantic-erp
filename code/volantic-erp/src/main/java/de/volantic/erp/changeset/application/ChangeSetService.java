package de.volantic.erp.changeset.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.audit.AuditTrail;
import de.volantic.erp.changeset.application.ChangeSetExceptions.ChangeSetNotFoundException;
import de.volantic.erp.changeset.application.ChangeSetExceptions.FieldNotEditableException;
import de.volantic.erp.changeset.application.port.out.ChangeSetStore;
import de.volantic.erp.changeset.domain.model.ChangeSet;
import de.volantic.erp.changeset.domain.model.ChangeSetId;
import de.volantic.erp.changeset.domain.model.ChangeSetMode;
import de.volantic.erp.changeset.domain.model.RecordedOperation;
import de.volantic.erp.core.entitylink.EntityRef;
import de.volantic.erp.core.revision.BulkEditHandler;
import de.volantic.erp.core.revision.ChangeOperation;
import de.volantic.erp.core.revision.ReversibleResourceHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private final ChangeSetStore store;
    private final ResourceHandlers handlers;
    private final AuditTrail audit;
    private final ObjectMapper json;

    ChangeSetService(ChangeSetStore store, ResourceHandlers handlers, AuditTrail audit, ObjectMapper json) {
        this.store = store;
        this.handlers = handlers;
        this.audit = audit;
        this.json = json;
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

        List<BulkPreview.Row> rows = new ArrayList<>();
        for (var id : change.ids()) {
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
        ChangeSet changeSet = load(session);
        BulkEditHandler bulk = handlers.bulkFor(change.resourceType());
        ReversibleResourceHandler reversible = handlers.reversibleFor(change.resourceType());
        rejectUneditableFields(change, bulk);

        String payload = serialize(change.fieldChanges());
        boolean deferred = changeSet.mode() == ChangeSetMode.DEFERRED;
        for (var id : change.ids()) {
            EntityRef target = EntityRef.of(change.resourceType(), id);
            if (deferred) {
                changeSet.record(new RecordedOperation(target, ChangeOperation.UPDATE, null, payload, OffsetDateTime.now()));
            } else {
                String before = reversible.capture(id);
                bulk.applyChange(id, change.fieldChanges());
                changeSet.record(new RecordedOperation(target, ChangeOperation.UPDATE, before, payload, OffsetDateTime.now()));
                audit.record("changeset.bulk-applied", change.resourceType(), id, payload);
            }
        }
        store.save(changeSet);
    }

    /** Closes a session: a Probemodus session applies its buffered operations ("Übertragen"). */
    @Transactional
    @PreAuthorize("hasPermission(null, 'changeset.bulk:execute')")
    public void commit(ChangeSetId session) {
        ChangeSet changeSet = load(session);
        if (changeSet.mode() == ChangeSetMode.DEFERRED) {
            for (RecordedOperation op : changeSet.operations()) {
                BulkEditHandler bulk = handlers.bulkFor(op.target().type());
                bulk.applyChange(op.target().id(), deserialize(op.payload()));
                audit.record("changeset.committed", op.target().type(), op.target().id(), op.payload());
            }
        }
        changeSet.commit();
        store.save(changeSet);
    }

    /** Throws away a Probemodus session before commit — nothing was applied. */
    @Transactional
    @PreAuthorize("hasPermission(null, 'changeset.bulk:execute')")
    public void discard(ChangeSetId session) {
        ChangeSet changeSet = load(session);
        changeSet.discard();
        store.save(changeSet);
    }

    /** Rollback Engine: takes a LIVE/committed session back via forward-only compensation. */
    @Transactional
    @PreAuthorize("hasPermission(null, 'changeset.rollback:revert')")
    public void revert(ChangeSetId session) {
        ChangeSet changeSet = load(session);
        for (RecordedOperation op : changeSet.operationsForReversal()) {
            ReversibleResourceHandler reversible = handlers.reversibleFor(op.target().type());
            reversible.compensate(op.operation(), op.target().id(), op.beforeState());
            audit.record("changeset.reverted", op.target().type(), op.target().id(), op.beforeState());
        }
        changeSet.revert();
        store.save(changeSet);
    }

    private ChangeSet load(ChangeSetId session) {
        return store.findById(session).orElseThrow(() -> new ChangeSetNotFoundException(session));
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
            throw new IllegalStateException("could not serialize field changes", e);
        }
    }

    private Map<String, String> deserialize(String payload) {
        try {
            return json.readValue(payload, FIELD_MAP);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not deserialize buffered field changes", e);
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
