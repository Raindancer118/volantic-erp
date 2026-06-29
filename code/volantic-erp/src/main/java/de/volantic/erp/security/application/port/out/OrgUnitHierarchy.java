package de.volantic.erp.security.application.port.out;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Outbound port that resolves the organizational-unit tree for authorization (ADR-0007). Implemented in
 * infrastructure (reading {@code security.org_unit}); kept behind a port so the authorization domain
 * stays free of persistence. The tree is small and changes rarely, so the adapter may cache it.
 */
public interface OrgUnitHierarchy {

    /**
     * The ancestor chain of a unit, from the unit itself up to the root: {@code [self, parent, …, root]}.
     * Empty if the unit is unknown. Used to decide whether a grant on an ancestor covers a requested
     * unit (downward coverage).
     */
    List<UUID> ancestorIds(UUID orgUnitId);

    /**
     * The given units together with all of their (transitive) descendants. Used to expand a user's
     * scoped grants into the full set of unit ids whose records they may see, for list filtering.
     * Unknown ids are returned as-is (they match only themselves).
     */
    Set<UUID> descendantIds(Set<UUID> orgUnitIds);
}
