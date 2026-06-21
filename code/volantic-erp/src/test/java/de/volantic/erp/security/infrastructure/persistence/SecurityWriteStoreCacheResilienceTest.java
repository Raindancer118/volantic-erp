package de.volantic.erp.security.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression test for the fail-open contract of the authorization-cache eviction. The eviction is a
 * manual cache access, so it bypasses {@code CacheConfig}'s {@code CacheErrorHandler} and must swallow
 * cache/Redis errors itself: a Redis outage must never fail an (already committed) security write — it
 * only degrades to the entry's TTL. Without this, every security write would fail while Redis is down.
 */
class SecurityWriteStoreCacheResilienceTest {

    private final AppUserJpaRepository users = mock(AppUserJpaRepository.class);
    private final RoleJpaRepository roles = mock(RoleJpaRepository.class);
    private final PermissionJpaRepository permissions = mock(PermissionJpaRepository.class);

    @SuppressWarnings("unchecked")
    private SecurityWriteStoreAdapter adapterWith(Cache cache) {
        CacheManager manager = mock(CacheManager.class);
        when(manager.getCache(CacheNames.USER_PERMISSIONS)).thenReturn(cache);
        ObjectProvider<CacheManager> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(manager);
        return new SecurityWriteStoreAdapter(users, roles, permissions, provider);
    }

    @Test
    void writeDoesNotFailWhenCacheEvictionThrows() {
        Cache redisDown = mock(Cache.class);
        doThrow(new RuntimeException("Unable to connect to Redis")).when(redisDown).evictIfPresent(anyString());

        SecurityWriteStoreAdapter adapter = adapterWith(redisDown);

        // No active transaction here, so eviction runs immediately; it must be swallowed, not propagated.
        assertThatCode(() -> adapter.createUser("subject-1", "alice", "alice@volantic.de"))
                .doesNotThrowAnyException();
    }

    @Test
    void writeStillSucceedsWithAReachableCache() {
        Cache reachable = mock(Cache.class);
        when(reachable.evictIfPresent(any())).thenReturn(true);

        SecurityWriteStoreAdapter adapter = adapterWith(reachable);

        assertThatCode(() -> adapter.createUser("subject-2", "bob", "bob@volantic.de"))
                .doesNotThrowAnyException();
    }
}
