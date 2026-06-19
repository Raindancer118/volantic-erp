package de.volantic.erp.crm.domain.model;

import java.util.UUID;

/**
 * Identifies the organizational unit a customer belongs to — the data scope used for fine-grained,
 * org-level authorization ({@code AccessScope} type {@code ORG_UNIT}). A user may hold a permission
 * globally (covers every org unit) or scoped to specific org units.
 *
 * <p>{@link #DEFAULT} is the well-known "headquarters" unit that existing customers are backfilled to
 * (see the Flyway migration) and that new customers fall back to when none is specified.
 */
public record OrgUnitId(UUID value) {

    /** Fixed id of the default/HQ org unit; mirrored by the migration backfill. */
    public static final OrgUnitId DEFAULT = new OrgUnitId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    public OrgUnitId {
        if (value == null) {
            throw new IllegalArgumentException("org unit id must not be null");
        }
    }
}
