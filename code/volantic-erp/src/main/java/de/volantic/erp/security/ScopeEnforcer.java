package de.volantic.erp.security;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Programmatic org-unit scope enforcement for the cases {@code @PreAuthorize("hasPermission(...)")}
 * cannot express declaratively (ADR-0007): instance-level checks that need the resource loaded first,
 * and list filtering. A thin, reusable wrapper over {@link AuthorizationService} that resolves the
 * acting subject from the current security context — so it works identically for direct REST calls, the
 * change-set bulk path (handlers call the domain services), and the four-eyes path (which impersonates
 * the original requester). Business services depend on this public port; they never touch the
 * security context or the org tree themselves (DRY).
 *
 * <p>For <em>create</em>, scope comes from the request and stays declarative on the service:
 * {@code @PreAuthorize("hasPermission(#orgUnitId, 'ORG_UNIT', 'crm.customer:create')")}.
 */
public interface ScopeEnforcer {

    /**
     * Asserts the current user may act under {@code permission} on a resource owned by the given org
     * unit (global, exact, or ancestor grant). Throws {@code AccessDeniedException} otherwise.
     */
    void require(String permission, UUID orgUnitId);

    /**
     * Asserts the current user holds {@code permission} in <em>some</em> scope — the gate for list
     * endpoints before they filter to {@link #permittedOrgUnits}. Throws {@code AccessDeniedException}
     * otherwise.
     */
    void requireAnywhere(String permission);

    /**
     * The org units the current user may see for {@code permission}, for repository filtering.
     * {@link Optional#empty()} means unrestricted (do not filter); a present (possibly empty) set is the
     * exact, descendant-expanded set of unit ids to filter to. Mirrors
     * {@link AuthorizationService#permittedOrgUnits}.
     */
    Optional<Set<UUID>> permittedOrgUnits(String permission);
}
