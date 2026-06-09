package de.volantic.erp.workflow.api;

import de.volantic.erp.workflow.application.ApprovalService;
import de.volantic.erp.workflow.domain.model.ApprovalInstance;
import de.volantic.erp.workflow.domain.model.ApprovalSubject;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST v1 endpoints for the generic approval workflow. Thin adapter: it maps DTOs and delegates to
 * {@link ApprovalService}, which enforces authorization. Versioned under {@code /v1} (additive-only).
 */
@RestController
@RequestMapping("/v1/workflow/approvals")
class ApprovalController {

    private final ApprovalService approvals;

    ApprovalController(ApprovalService approvals) {
        this.approvals = approvals;
    }

    @PostMapping
    ResponseEntity<ApprovalInstanceResponse> start(@Valid @RequestBody StartApprovalRequest request) {
        ApprovalInstance started = approvals.requestApproval(
                new ApprovalSubject(request.subjectType(), request.subjectId()), request.requestedBy());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(started.instanceId()).toUri();
        return ResponseEntity.created(location).body(ApprovalInstanceResponse.from(started));
    }

    @GetMapping("/{instanceId}")
    ApprovalInstanceResponse getById(@PathVariable String instanceId) {
        return ApprovalInstanceResponse.from(approvals.getInstance(instanceId));
    }

    @GetMapping("/pending")
    List<PendingApprovalResponse> pending() {
        return approvals.pendingApprovals().stream()
                .map(PendingApprovalResponse::from)
                .toList();
    }

    @PostMapping("/tasks/{taskId}/decision")
    ResponseEntity<Void> decide(@PathVariable String taskId, @Valid @RequestBody DecisionRequest request) {
        approvals.decide(taskId, request.decision(), request.decidedBy());
        return ResponseEntity.noContent().build();
    }
}
