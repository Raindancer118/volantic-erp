package de.volantic.erp.changeset.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.audit.AuditTrail;
import de.volantic.erp.changeset.application.ChangeSetExceptions.ChangeSetAccessDeniedException;
import de.volantic.erp.changeset.application.ChangeSetExceptions.ChangeSetNotFoundException;
import de.volantic.erp.changeset.application.ChangeSetExceptions.FieldNotEditableException;
import de.volantic.erp.changeset.application.port.out.ChangeSetStore;
import de.volantic.erp.changeset.domain.model.ChangeSet;
import de.volantic.erp.changeset.domain.model.ChangeSetId;
import de.volantic.erp.changeset.domain.model.ChangeSetMode;
import de.volantic.erp.changeset.domain.model.ChangeSetStatus;
import de.volantic.erp.changeset.domain.model.RecordedOperation;
import de.volantic.erp.core.entitylink.EntityRef;
import de.volantic.erp.core.revision.BulkEditHandler;
import de.volantic.erp.core.revision.ChangeOperation;
import de.volantic.erp.core.revision.ReversibleResourceHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Use-case orchestration of {@link ChangeSetService} over mocked ports/handlers: LIVE vs Probemodus
 * (DEFERRED) apply, commit ("Übertragen"), Rollback Engine revert, discard, preview and error paths.
 */
class ChangeSetServiceTest {

    private static final String TYPE = "crm.customer";

    private final ChangeSetStore store = mock(ChangeSetStore.class);
    private final BulkEditHandler bulkHandler = mock(BulkEditHandler.class);
    private final ReversibleResourceHandler revHandler = mock(ReversibleResourceHandler.class);
    private final AuditTrail audit = mock(AuditTrail.class);
    private final ObjectMapper json = new ObjectMapper();

    private final UUID id1 = UUID.randomUUID();
    private final UUID id2 = UUID.randomUUID();
    private final Map<String, String> changes = Map.of("name", "Neo");

    private ChangeSetService service;

    @BeforeEach
    void setUp() {
        when(bulkHandler.resourceType()).thenReturn(TYPE);
        when(bulkHandler.editableFields()).thenReturn(Set.of("name", "email"));
        when(revHandler.resourceType()).thenReturn(TYPE);
        ResourceHandlers handlers = new ResourceHandlers(List.of(bulkHandler), List.of(revHandler));
        service = new ChangeSetService(store, handlers, audit, json);
        // The sessions under test are opened by "alice"; authenticate as her so the ownership check passes.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null, AuthorityUtils.NO_AUTHORITIES));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private BulkChange change(Map<String, String> fields, UUID... ids) {
        return new BulkChange(TYPE, List.of(ids), fields);
    }

    private void given(ChangeSet session) {
        when(store.findById(session.id())).thenReturn(Optional.of(session));
    }

    @Test
    void beginLivePersistsAnOpenLiveSession() {
        ChangeSetId id = service.beginLive();

        ArgumentCaptor<ChangeSet> saved = ArgumentCaptor.forClass(ChangeSet.class);
        verify(store).save(saved.capture());
        assertThat(saved.getValue().mode()).isEqualTo(ChangeSetMode.LIVE);
        assertThat(saved.getValue().status()).isEqualTo(ChangeSetStatus.OPEN);
        assertThat(id).isEqualTo(saved.getValue().id());
    }

    @Test
    void beginProbemodusPersistsAnOpenDeferredSession() {
        service.beginProbemodus();

        ArgumentCaptor<ChangeSet> saved = ArgumentCaptor.forClass(ChangeSet.class);
        verify(store).save(saved.capture());
        assertThat(saved.getValue().mode()).isEqualTo(ChangeSetMode.DEFERRED);
    }

    @Test
    void applyLiveCapturesBeforeStateAppliesChangeRecordsAndAudits() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.LIVE);
        given(session);
        when(revHandler.capture(id1)).thenReturn("BEFORE-1");

        service.apply(session.id(), change(changes, id1));

        verify(revHandler).capture(id1);
        verify(bulkHandler).applyChange(id1, changes);
        verify(audit).record(anyString(), eq(TYPE), eq(id1), anyString());
        verify(store).save(session);
        assertThat(session.operations()).singleElement().satisfies(op -> {
            assertThat(op.operation()).isEqualTo(ChangeOperation.UPDATE);
            assertThat(op.beforeState()).isEqualTo("BEFORE-1");
            assertThat(op.target()).isEqualTo(EntityRef.of(TYPE, id1));
        });
    }

    @Test
    void applyProbemodusBuffersWithoutTouchingTheResource() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.DEFERRED);
        given(session);

        service.apply(session.id(), change(changes, id1));

        verify(bulkHandler, never()).applyChange(any(), any());
        verify(revHandler, never()).capture(any());
        assertThat(session.operations()).singleElement().satisfies(op -> {
            assertThat(op.beforeState()).isNull();
            assertThat(op.payload()).contains("name").contains("Neo");
        });
    }

    @Test
    void commitProbemodusAppliesBufferedOperations() throws Exception {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.DEFERRED);
        session.record(new RecordedOperation(EntityRef.of(TYPE, id1), ChangeOperation.UPDATE,
                null, json.writeValueAsString(changes), OffsetDateTime.now()));
        given(session);

        service.commit(session.id());

        verify(bulkHandler).applyChange(id1, changes);
        verify(store).save(session);
        assertThat(session.status()).isEqualTo(ChangeSetStatus.COMMITTED);
    }

    @Test
    void commitLiveJustClosesWithoutReapplying() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.LIVE);
        given(session);

        service.commit(session.id());

        verify(bulkHandler, never()).applyChange(any(), any());
        assertThat(session.status()).isEqualTo(ChangeSetStatus.COMMITTED);
    }

    @Test
    void revertCompensatesEveryOperationNewestFirst() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.LIVE);
        session.record(new RecordedOperation(EntityRef.of(TYPE, id1), ChangeOperation.UPDATE,
                "B1", null, OffsetDateTime.now()));
        session.record(new RecordedOperation(EntityRef.of(TYPE, id2), ChangeOperation.UPDATE,
                "B2", null, OffsetDateTime.now()));
        given(session);

        service.revert(session.id());

        InOrder inOrder = inOrder(revHandler);
        inOrder.verify(revHandler).compensate(ChangeOperation.UPDATE, id2, "B2");
        inOrder.verify(revHandler).compensate(ChangeOperation.UPDATE, id1, "B1");
        assertThat(session.status()).isEqualTo(ChangeSetStatus.REVERTED);
    }

    @Test
    void discardMarksTheProbemodusSessionDiscarded() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.DEFERRED);
        given(session);

        service.discard(session.id());

        assertThat(session.status()).isEqualTo(ChangeSetStatus.DISCARDED);
    }

    @Test
    void previewReportsCurrentStateForEditableFields() {
        when(revHandler.capture(id1)).thenReturn("CURRENT");

        BulkPreview preview = service.preview(change(changes, id1));

        assertThat(preview.hasProblems()).isFalse();
        assertThat(preview.rows()).singleElement().satisfies(row -> {
            assertThat(row.applicable()).isTrue();
            assertThat(row.beforeState()).isEqualTo("CURRENT");
        });
    }

    @Test
    void previewFlagsUneditableFieldsAsNotApplicable() {
        when(revHandler.capture(id1)).thenReturn("CURRENT");

        BulkPreview preview = service.preview(change(Map.of("secret", "x"), id1));

        assertThat(preview.hasProblems()).isTrue();
        assertThat(preview.rows()).allSatisfy(row -> assertThat(row.applicable()).isFalse());
    }

    @Test
    void applyRejectsUneditableFields() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.LIVE);
        given(session);

        assertThatThrownBy(() -> service.apply(session.id(), change(Map.of("secret", "x"), id1)))
                .isInstanceOf(FieldNotEditableException.class);
    }

    @Test
    void operationsOnUnknownSessionAreRejected() {
        when(store.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply(ChangeSetId.newId(), change(changes, id1)))
                .isInstanceOf(ChangeSetNotFoundException.class);
    }

    @Test
    void operatingOnAnotherActorsSessionIsRejected() {
        ChangeSet foreign = ChangeSet.open("mallory", ChangeSetMode.DEFERRED);
        given(foreign);

        // "alice" (the authenticated actor) must not touch a session owned by "mallory".
        assertThatThrownBy(() -> service.commit(foreign.id())).isInstanceOf(ChangeSetAccessDeniedException.class);
        assertThatThrownBy(() -> service.discard(foreign.id())).isInstanceOf(ChangeSetAccessDeniedException.class);
        assertThatThrownBy(() -> service.revert(foreign.id())).isInstanceOf(ChangeSetAccessDeniedException.class);
        assertThatThrownBy(() -> service.apply(foreign.id(), change(changes, id1)))
                .isInstanceOf(ChangeSetAccessDeniedException.class);
        verify(bulkHandler, never()).applyChange(any(), any());
    }

    @Test
    void commitProbemodusCapturesBeforeStateSoItCanBeReverted() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.DEFERRED);
        given(session);
        service.apply(session.id(), change(changes, id1));     // buffers, before-state still null
        when(revHandler.capture(id1)).thenReturn("BEFORE-AT-COMMIT");

        service.commit(session.id());

        // The committed operation now carries the before-state captured while applying, so a later
        // revert can compensate it instead of writing null.
        assertThat(session.operations()).singleElement()
                .satisfies(op -> assertThat(op.beforeState()).isEqualTo("BEFORE-AT-COMMIT"));
    }

    @Test
    void getSessionReturnsTheActorsOwnSession() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.LIVE);
        given(session);

        assertThat(service.getSession(session.id())).isEqualTo(session);
    }

    @Test
    void getSessionOfAnotherActorIsDenied() {
        ChangeSet foreign = ChangeSet.open("bob", ChangeSetMode.LIVE);
        given(foreign);

        assertThatThrownBy(() -> service.getSession(foreign.id()))
                .isInstanceOf(ChangeSetAccessDeniedException.class);
    }

    @Test
    void getSessionUnknownThrowsNotFound() {
        ChangeSetId unknown = ChangeSetId.newId();
        when(store.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSession(unknown)).isInstanceOf(ChangeSetNotFoundException.class);
    }

    @Test
    void listSessionsQueriesOnlyTheCurrentActorsSessions() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.LIVE);
        Pageable pageable = PageRequest.of(0, 20);
        when(store.findByActor("alice", pageable)).thenReturn(new PageImpl<>(List.of(session)));

        assertThat(service.listSessions(pageable).getContent()).containsExactly(session);
        verify(store).findByActor(eq("alice"), eq(pageable));
    }
}
