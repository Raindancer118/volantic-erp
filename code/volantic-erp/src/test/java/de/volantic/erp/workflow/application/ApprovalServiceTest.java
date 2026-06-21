package de.volantic.erp.workflow.application;

import de.volantic.erp.workflow.ApprovalDecided;
import de.volantic.erp.workflow.application.port.out.ApprovalEngine;
import de.volantic.erp.workflow.domain.model.ApprovalDecision;
import de.volantic.erp.workflow.domain.model.ApprovalInstance;
import de.volantic.erp.workflow.domain.model.ApprovalStatus;
import de.volantic.erp.workflow.domain.model.ApprovalSubject;
import de.volantic.erp.workflow.domain.model.PendingApproval;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Use-case logic of {@link ApprovalService} over a mocked {@link ApprovalEngine} (no Spring/engine). */
class ApprovalServiceTest {

    private final ApprovalEngine engine = mock(ApprovalEngine.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ApprovalService service = new ApprovalService(engine, events);

    @Test
    void requestDelegatesToEngine() {
        ApprovalSubject subject = new ApprovalSubject("purchase-order", "42");
        ApprovalInstance instance = new ApprovalInstance("p-1", subject, ApprovalStatus.PENDING);
        when(engine.start(subject, "alice")).thenReturn(instance);

        ApprovalInstance result = service.requestApproval(subject, "alice");

        assertThat(result).isEqualTo(instance);
    }

    @Test
    void getInstanceThrowsWhenMissing() {
        when(engine.findInstance("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getInstance("missing"))
                .isInstanceOf(WorkflowExceptions.ApprovalInstanceNotFound.class);
    }

    @Test
    void decideDelegatesToEngineAndPublishesTheOutcomeForTheSubject() {
        ApprovalSubject subject = new ApprovalSubject("changeset.session", "s-1");
        when(engine.pendingApprovals()).thenReturn(List.of(new PendingApproval("t-1", "p-1", subject)));

        service.decide("t-1", ApprovalDecision.APPROVED, "bob");

        verify(engine).decide(eq("t-1"), eq(ApprovalDecision.APPROVED), eq("bob"));
        verify(events).publishEvent(new ApprovalDecided("changeset.session", "s-1", true, "bob"));
    }

    @Test
    void requestApprovalViaThePortReturnsTheInstanceId() {
        ApprovalSubject subject = new ApprovalSubject("changeset.session", "s-2");
        when(engine.start(eq(subject), eq("alice")))
                .thenReturn(new ApprovalInstance("p-2", subject, ApprovalStatus.PENDING));

        assertThat(service.requestApproval("changeset.session", "s-2", "alice")).isEqualTo("p-2");
    }
}
