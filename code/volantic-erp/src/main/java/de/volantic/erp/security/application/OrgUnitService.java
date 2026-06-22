package de.volantic.erp.security.application;

import de.volantic.erp.audit.AuditTrail;
import de.volantic.erp.core.OptimisticLock;
import de.volantic.erp.security.application.port.out.OrgUnitRepository;
import de.volantic.erp.security.domain.model.OrgUnit;
import de.volantic.erp.security.domain.model.OrgUnitId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Org-unit management use cases (ADR-0007). Managing the scope tree is an administrative function, so it
 * is gated by <em>global</em> {@code security.orgunit:*} permissions (not itself org-scoped). Changes are
 * recorded in the tamper-evident {@link AuditTrail}. Reparenting an existing unit is intentionally not
 * offered yet (it needs cycle re-validation across the tree) — documented as a follow-up in ADR-0007.
 */
@Service
public class OrgUnitService {

    private final OrgUnitRepository orgUnits;
    private final AuditTrail auditTrail;

    OrgUnitService(OrgUnitRepository orgUnits, AuditTrail auditTrail) {
        this.orgUnits = orgUnits;
        this.auditTrail = auditTrail;
    }

    @Transactional
    @PreAuthorize("hasPermission(null, 'security.orgunit:create')")
    public OrgUnit createOrgUnit(String code, String name, OrgUnitId parentId) {
        if (orgUnits.existsByCode(code)) {
            throw new OrgUnitCodeAlreadyExistsException(code);
        }
        if (parentId != null && !orgUnits.existsById(parentId)) {
            throw new OrgUnitNotFoundException(parentId);
        }
        OrgUnit created = orgUnits.save(OrgUnit.create(code, name, parentId));
        auditTrail.record("security.orgunit-created", "security.org_unit",
                created.id().value(), code + (parentId == null ? " (root)" : " under " + parentId.value()));
        return created;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'security.orgunit:read')")
    public OrgUnit getOrgUnit(OrgUnitId id) {
        return orgUnits.findById(id).orElseThrow(() -> new OrgUnitNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'security.orgunit:read')")
    public Page<OrgUnit> listOrgUnits(Pageable pageable) {
        return orgUnits.findAll(pageable);
    }

    /** Renames a unit (the {@code code} business key is immutable, like other aggregates' keys). */
    @Transactional
    @PreAuthorize("hasPermission(null, 'security.orgunit:update')")
    public OrgUnit renameOrgUnit(OrgUnitId id, long expectedVersion, String name) {
        OrgUnit orgUnit = orgUnits.findById(id).orElseThrow(() -> new OrgUnitNotFoundException(id));
        OptimisticLock.check(orgUnit.version(), expectedVersion, id.value());
        orgUnit.rename(name);
        OrgUnit saved = orgUnits.save(orgUnit);
        auditTrail.record("security.orgunit-renamed", "security.org_unit", id.value(), name);
        return saved;
    }
}
