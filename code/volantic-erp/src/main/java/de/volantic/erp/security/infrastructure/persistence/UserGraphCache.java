package de.volantic.erp.security.infrastructure.persistence;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Loads and caches the per-OIDC-subject authorization snapshot ({@link CachedUser}) in Redis
 * (cache {@link CacheNames#USER_PERMISSIONS}). On a cache hit the database is not touched at all,
 * which keeps the authorization hot path within the sub-500 ms budget (NFR).
 *
 * <p>The actual DB read uses {@code findWithRolesByOidcSubject} (single query via {@code @EntityGraph}),
 * so even a cache miss is N+1-free. Unknown subjects return {@code null} and are <em>not</em> cached
 * ({@code unless}), to avoid filling the cache with bogus keys.
 *
 * <p>TODO: explicit eviction once a write side exists (role/assignment changes). Until then the short
 * TTL in {@code CacheConfig} bounds staleness.
 */
@Component
class UserGraphCache {

    private final AppUserJpaRepository users;

    UserGraphCache(AppUserJpaRepository users) {
        this.users = users;
    }

    @Cacheable(cacheNames = CacheNames.USER_PERMISSIONS, key = "#oidcSubject", unless = "#result == null")
    public CachedUser load(String oidcSubject) {
        return users.findWithRolesByOidcSubject(oidcSubject).map(CachedUser::from).orElse(null);
    }
}
