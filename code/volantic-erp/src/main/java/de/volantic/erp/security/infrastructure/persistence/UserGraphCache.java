package de.volantic.erp.security.infrastructure.persistence;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Loads and caches the per-OIDC-subject authorization snapshot ({@link CachedUser}) in Redis
 * (cache {@link CacheNames#USER_PERMISSIONS}). On a cache hit the database is not touched at all,
 * which keeps the authorization hot path within the sub-500 ms budget (NFR).
 *
 * <p>The actual DB read uses {@code findWithRolesByOidcSubject} (single query via {@code @EntityGraph}),
 * so even a cache miss is N+1-free.
 *
 * <p>Unknown subjects ({@code null}) are <strong>also cached</strong> on purpose: a validly signed but
 * not-yet-mirrored OIDC subject would otherwise hit the database on every request, which an attacker
 * holding any valid token could abuse to bypass the cache. Negative caching shields the database; the
 * short TTL in {@code CacheConfig} bounds staleness.
 *
 * <p>Eviction: the security write side ({@code SecurityWriteStoreAdapter}) evicts a subject's entry
 * after commit on every authorization-relevant change (provisioning, status, role definition/assignment),
 * fail-open if the cache is unreachable; the short TTL bounds any residual staleness.
 */
@Component
class UserGraphCache {

    private final AppUserJpaRepository users;

    UserGraphCache(AppUserJpaRepository users) {
        this.users = users;
    }

    @Cacheable(cacheNames = CacheNames.USER_PERMISSIONS, key = "#oidcSubject")
    public CachedUser load(String oidcSubject) {
        return users.findWithRolesByOidcSubject(oidcSubject).map(CachedUser::from).orElse(null);
    }
}
