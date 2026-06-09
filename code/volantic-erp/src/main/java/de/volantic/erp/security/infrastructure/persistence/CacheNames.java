package de.volantic.erp.security.infrastructure.persistence;

/** Cache names used by the security module. Referenced by {@code @Cacheable} and the cache config. */
public final class CacheNames {

    /** Per-OIDC-subject authorization snapshot ({@link CachedUser}). */
    public static final String USER_PERMISSIONS = "security:user-permissions";

    private CacheNames() {
    }
}
