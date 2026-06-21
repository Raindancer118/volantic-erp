package de.volantic.erp.changeset.infrastructure.approval;

import de.volantic.erp.changeset.application.ChangeSetService;
import de.volantic.erp.changeset.application.port.out.ChangeSetStore;
import de.volantic.erp.changeset.domain.model.ChangeSet;
import de.volantic.erp.changeset.domain.model.ChangeSetMode;
import de.volantic.erp.workflow.ApprovalDecided;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Unit tests for the approval listener: subject routing and running the outcome as the session's actor. */
class ChangeSetApprovalListenerTest {

    private final ChangeSetService service = mock(ChangeSetService.class);
    private final ChangeSetStore store = mock(ChangeSetStore.class);
    private final ChangeSetApprovalListener listener = new ChangeSetApprovalListener(service, store);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void appliesTheOutcomeAsTheOriginalRequesterAndRestoresTheContext() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.DEFERRED);
        when(store.findById(session.id())).thenReturn(Optional.of(session));
        // The buffered writes authorize against the requester, so the context must be 'alice' at call time.
        doAnswer(invocation -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice");
            return null;
        }).when(service).applyApprovalOutcome(eq(session.id()), eq(true));

        listener.on(new ApprovalDecided("changeset.session", session.id().value().toString(), true, "bob"));

        verify(service).applyApprovalOutcome(eq(session.id()), eq(true));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull(); // restored afterwards
    }

    @Test
    void ignoresEventsForOtherSubjectTypes() {
        listener.on(new ApprovalDecided("purchase-order", UUID.randomUUID().toString(), true, "bob"));

        verifyNoInteractions(service, store);
    }
}
