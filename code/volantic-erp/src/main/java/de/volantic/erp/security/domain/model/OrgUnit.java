package de.volantic.erp.security.domain.model;

import de.volantic.erp.core.UuidV7;

/**
 * An organizational unit — a node in the hierarchical scope tree (ADR-0007). Pure domain: owns its
 * identity and invariants, free of persistence and framework concerns. A unit with {@code parentId ==
 * null} is a root. The {@code code} is the human-facing business key (unique). Cycle-freeness across
 * the whole tree (a unit must not become its own ancestor) is enforced in the application service,
 * which alone can see the tree.
 */
public final class OrgUnit {

    private final OrgUnitId id;
    private final Long version;
    private final OrgUnitId parentId;
    private final String code;
    private String name;

    private OrgUnit(OrgUnitId id, Long version, OrgUnitId parentId, String code, String name) {
        this.id = id;
        this.version = version;
        this.parentId = parentId;
        this.code = requireText(code, "code");
        this.name = requireText(name, "name");
        if (parentId != null && parentId.equals(id)) {
            throw new IllegalArgumentException("an org unit cannot be its own parent");
        }
    }

    /** Creates a brand-new org unit with a fresh identity (no version yet). {@code parentId} null = root. */
    public static OrgUnit create(String code, String name, OrgUnitId parentId) {
        return new OrgUnit(new OrgUnitId(UuidV7.randomUuid()), null, parentId, code, name);
    }

    /** Re-creates an existing org unit including its optimistic-lock version (used by the adapter). */
    public static OrgUnit reconstitute(OrgUnitId id, long version, OrgUnitId parentId, String code, String name) {
        return new OrgUnit(id, version, parentId, code, name);
    }

    public void rename(String newName) {
        this.name = requireText(newName, "name");
    }

    public OrgUnitId id() {
        return id;
    }

    /** Optimistic-lock version this aggregate was loaded at; {@code null} for a not-yet-persisted one. */
    public Long version() {
        return version;
    }

    /** Parent unit id, or {@code null} for a root unit. */
    public OrgUnitId parentId() {
        return parentId;
    }

    public boolean isRoot() {
        return parentId == null;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OrgUnit that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }
}
