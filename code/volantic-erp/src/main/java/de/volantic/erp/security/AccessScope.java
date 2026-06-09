package de.volantic.erp.security;

import java.util.UUID;

/**
 * Data scope of an authorization request or grant (instance/area level).
 *
 * <p>{@link #GLOBAL} means "unrestricted". A concrete scope binds a permission to an instance/area
 * (e.g. {@code AccessScope.of("DEPT", departmentId)}). A globally granted permission covers every
 * requested scope; a scoped grant covers only its own exact scope — and in particular no global
 * request.
 */
public record AccessScope(String type, UUID id) {

    public static final AccessScope GLOBAL = new AccessScope("GLOBAL", null);

    public static AccessScope of(String type, UUID id) {
        return new AccessScope(type, id);
    }

    public boolean isGlobal() {
        return id == null;
    }
}
