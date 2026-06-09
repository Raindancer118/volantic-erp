package de.volantic.erp.workflow.api;

import de.volantic.erp.workflow.domain.model.ApprovalDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body to record a decision on an open approval task (REST v1). */
public record DecisionRequest(
        @NotNull ApprovalDecision decision,
        @NotBlank @Size(max = 255) String decidedBy) {
}
