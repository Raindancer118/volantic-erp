package de.volantic.erp.workflow.application;

import de.volantic.erp.workflow.application.port.out.ApprovalEngine;
import de.volantic.erp.workflow.domain.model.ApprovalDecision;
import de.volantic.erp.workflow.domain.model.ApprovalInstance;
import de.volantic.erp.workflow.domain.model.ApprovalStatus;
import de.volantic.erp.workflow.domain.model.ApprovalSubject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Use-case logic of {@link ApprovalService} over a mocked {@link ApprovalEngine} (no Spring/engine). */
class ApprovalServiceTest {

    private final ApprovalEngine engine = mock(ApprovalEngine.class);
    private final ApprovalService service = new ApprovalService(engine);

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
    void decideDelegatesToEngine() {
        service.decide("t-1", ApprovalDecision.APPROVED, "bob");

        verify(engine).decide(eq("t-1"), eq(ApprovalDecision.APPROVED), eq("bob"));
    }
}
