package de.volantic.erp.audit.api;

import de.volantic.erp.audit.application.AuditService;
import de.volantic.erp.audit.application.IntegrityResult;
import de.volantic.erp.core.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST v1 read endpoints for the audit trail. The log is append-only — there is intentionally no write
 * endpoint; modules record through the {@link de.volantic.erp.audit.AuditTrail} API. Gated by
 * {@code audit.log:read}.
 */
@RestController
@RequestMapping("/v1/audit")
class AuditController {

    private final AuditService audit;

    AuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping("/log")
    @PreAuthorize("hasPermission(null, 'audit.log:read')")
    PageResponse<AuditEntryResponse> log(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(audit.entries(pageable), AuditEntryResponse::from);
    }

    /** Recomputes the hash chain and reports whether it is intact (and where it broke, if not). */
    @GetMapping("/verify")
    @PreAuthorize("hasPermission(null, 'audit.log:read')")
    IntegrityResult verify() {
        return audit.verifyIntegrity();
    }
}
