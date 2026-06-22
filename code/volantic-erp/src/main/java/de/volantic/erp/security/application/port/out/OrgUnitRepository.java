package de.volantic.erp.security.application.port.out;

import de.volantic.erp.security.domain.model.OrgUnit;
import de.volantic.erp.security.domain.model.OrgUnitId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Outbound port for org-unit persistence. The implementation (infrastructure) maps between the pure
 * domain {@link OrgUnit} and its JPA representation, and evicts the cached org tree on writes.
 */
public interface OrgUnitRepository {

    OrgUnit save(OrgUnit orgUnit);

    Optional<OrgUnit> findById(OrgUnitId id);

    boolean existsByCode(String code);

    boolean existsById(OrgUnitId id);

    Page<OrgUnit> findAll(Pageable pageable);
}
