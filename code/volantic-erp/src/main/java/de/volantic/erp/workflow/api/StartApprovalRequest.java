package de.volantic.erp.workflow.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body to start an approval process for a subject (REST v1). */
public record StartApprovalRequest(
        @NotBlank @Size(max = 100) String subjectType,
        @NotBlank @Size(max = 100) String subjectId,
        @NotBlank @Size(max = 255) String requestedBy) {
}
