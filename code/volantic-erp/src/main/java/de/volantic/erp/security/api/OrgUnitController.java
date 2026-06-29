package de.volantic.erp.security.api;

import de.volantic.erp.core.web.ETags;
import de.volantic.erp.core.web.PageResponse;
import de.volantic.erp.security.application.OrgUnitService;
import de.volantic.erp.security.domain.model.OrgUnit;
import de.volantic.erp.security.domain.model.OrgUnitId;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST v1 endpoints for organizational units (ADR-0007). Thin adapter: maps DTOs and delegates to
 * {@link OrgUnitService}, which enforces authorization. The tree is built client-side from each unit's
 * {@code parentId}.
 */
@RestController
@RequestMapping("/v1/security/org-units")
class OrgUnitController {

    private final OrgUnitService orgUnits;

    OrgUnitController(OrgUnitService orgUnits) {
        this.orgUnits = orgUnits;
    }

    @PostMapping
    ResponseEntity<OrgUnitResponse> create(@Valid @RequestBody CreateOrgUnitRequest request) {
        OrgUnitId parentId = request.parentId() == null ? null : new OrgUnitId(request.parentId());
        OrgUnit created = orgUnits.createOrgUnit(request.code(), request.name(), parentId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id().value()).toUri();
        OrgUnitResponse body = OrgUnitResponse.from(created);
        return ResponseEntity.created(location).eTag(ETags.format(body.version())).body(body);
    }

    @GetMapping("/{id}")
    ResponseEntity<OrgUnitResponse> getById(@PathVariable UUID id) {
        return withETag(OrgUnitResponse.from(orgUnits.getOrgUnit(new OrgUnitId(id))));
    }

    @GetMapping
    PageResponse<OrgUnitResponse> list(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(orgUnits.listOrgUnits(pageable), OrgUnitResponse::from);
    }

    @PutMapping("/{id}")
    ResponseEntity<OrgUnitResponse> rename(@PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody RenameOrgUnitRequest request) {
        OrgUnit updated = orgUnits.renameOrgUnit(new OrgUnitId(id), ETags.parseIfMatch(ifMatch), request.name());
        return withETag(OrgUnitResponse.from(updated));
    }

    private static ResponseEntity<OrgUnitResponse> withETag(OrgUnitResponse body) {
        return ResponseEntity.ok().eTag(ETags.format(body.version())).body(body);
    }
}
