package de.volantic.erp.changeset.api;

import de.volantic.erp.changeset.application.ChangeSetService;
import de.volantic.erp.changeset.domain.model.ChangeSetId;
import de.volantic.erp.changeset.domain.model.ChangeSetMode;
import de.volantic.erp.core.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST v1 endpoints for the Rollback Engine and the Probemodus (ADR-0006). Thin adapter: it maps DTOs and
 * delegates to {@link ChangeSetService}, which enforces authorization and ownership. Versioned under
 * {@code /v1} (additive-only policy). A session is begun, mass edits are previewed and applied within it,
 * and it is finally committed (Übertragen), discarded or reverted (Rollback Engine).
 */
@RestController
@RequestMapping("/v1/changeset")
class ChangeSetController {

    private final ChangeSetService changeSets;

    ChangeSetController(ChangeSetService changeSets) {
        this.changeSets = changeSets;
    }

    @PostMapping("/sessions")
    ResponseEntity<SessionResponse> begin(@Valid @RequestBody BeginSessionRequest request) {
        ChangeSetMode mode = request.mode();
        ChangeSetId id = mode == ChangeSetMode.LIVE ? changeSets.beginLive() : changeSets.beginProbemodus();
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(id.value()).toUri();
        return ResponseEntity.created(location).body(SessionResponse.of(id, mode));
    }

    @GetMapping("/sessions")
    PageResponse<ChangeSetSummaryResponse> list(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(changeSets.listSessions(pageable), ChangeSetSummaryResponse::from);
    }

    @GetMapping("/sessions/{id}")
    ChangeSetDetailResponse getById(@PathVariable UUID id) {
        return ChangeSetDetailResponse.from(changeSets.getSession(new ChangeSetId(id)));
    }

    @PostMapping("/preview")
    BulkPreviewResponse preview(@Valid @RequestBody BulkChangeRequest request) {
        return BulkPreviewResponse.from(changeSets.preview(request.toCommand()));
    }

    @PostMapping("/sessions/{id}/apply")
    ResponseEntity<Void> apply(@PathVariable UUID id, @Valid @RequestBody BulkChangeRequest request) {
        changeSets.apply(new ChangeSetId(id), request.toCommand());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{id}/commit")
    ResponseEntity<Void> commit(@PathVariable UUID id) {
        changeSets.commit(new ChangeSetId(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{id}/discard")
    ResponseEntity<Void> discard(@PathVariable UUID id) {
        changeSets.discard(new ChangeSetId(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{id}/revert")
    ResponseEntity<Void> revert(@PathVariable UUID id) {
        changeSets.revert(new ChangeSetId(id));
        return ResponseEntity.noContent().build();
    }
}
