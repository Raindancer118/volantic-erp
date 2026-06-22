package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.security.application.port.out.OrgUnitRepository;
import de.volantic.erp.security.domain.model.OrgUnit;
import de.volantic.erp.security.domain.model.OrgUnitId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound adapter for {@link OrgUnitRepository}: maps between the pure domain {@link OrgUnit} and the
 * JPA {@link OrgUnitEntity}, and evicts the cached org tree ({@link OrgTreeCache}) after a write so
 * hierarchy resolution sees the change. A versioned aggregate is saved as a version-checked merge
 * (optimistic locking) without re-fetching first.
 */
@Component
class OrgUnitRepositoryAdapter implements OrgUnitRepository {

    private final OrgUnitJpaRepository jpa;
    private final OrgTreeCache treeCache;

    OrgUnitRepositoryAdapter(OrgUnitJpaRepository jpa, OrgTreeCache treeCache) {
        this.jpa = jpa;
        this.treeCache = treeCache;
    }

    @Override
    public OrgUnit save(OrgUnit orgUnit) {
        UUID parentId = orgUnit.parentId() == null ? null : orgUnit.parentId().value();
        OrgUnitEntity entity = orgUnit.version() == null
                ? new OrgUnitEntity(orgUnit.id().value(), parentId, orgUnit.code(), orgUnit.name())
                : OrgUnitEntity.forUpdate(orgUnit.id().value(), parentId, orgUnit.code(),
                        orgUnit.name(), orgUnit.version());
        OrgUnit saved = toDomain(jpa.save(entity));
        treeCache.evictAfterCommit();
        return saved;
    }

    @Override
    public Optional<OrgUnit> findById(OrgUnitId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }

    @Override
    public boolean existsById(OrgUnitId id) {
        return jpa.existsById(id.value());
    }

    @Override
    public Page<OrgUnit> findAll(Pageable pageable) {
        return jpa.findAll(pageable).map(this::toDomain);
    }

    private OrgUnit toDomain(OrgUnitEntity entity) {
        OrgUnitId parentId = entity.parentId() == null ? null : new OrgUnitId(entity.parentId());
        return OrgUnit.reconstitute(new OrgUnitId(entity.getId()), entity.getVersion(),
                parentId, entity.code(), entity.name());
    }
}
