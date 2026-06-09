package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.domain.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that write operations invalidate the authorization cache. Runs without an active
 * transaction, so the after-commit hook evicts immediately (the production after-commit path is the
 * standard Spring {@code TransactionSynchronization}).
 */
class SecurityWriteStoreAdapterEvictionTest {

    private final AppUserJpaRepository users = mock(AppUserJpaRepository.class);
    private final RoleJpaRepository roles = mock(RoleJpaRepository.class);
    private final PermissionJpaRepository permissions = mock(PermissionJpaRepository.class);
    private final CacheManager cacheManager = new ConcurrentMapCacheManager(CacheNames.USER_PERMISSIONS);

    private SecurityWriteStoreAdapter adapter;
    private Cache cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<CacheManager> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(cacheManager);
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        adapter = new SecurityWriteStoreAdapter(users, roles, permissions, provider);
        cache = cacheManager.getCache(CacheNames.USER_PERMISSIONS);
        cache.put("sub-1", new CachedUser("sub-1", "ACTIVE", List.of()));
        cache.put("sub-2", new CachedUser("sub-2", "ACTIVE", List.of()));
    }

    @Test
    void createUserEvictsThatSubject() {
        adapter.createUser("sub-1", "m.muster", "m@example.de");

        assertThat(cache.get("sub-1")).isNull();
        assertThat(cache.get("sub-2")).isNotNull();
    }

    @Test
    void setUserStatusEvictsThatSubject() {
        when(users.findByOidcSubject("sub-1")).thenReturn(Optional.of(new AppUserEntity("sub-1", "m", "m@e")));

        adapter.setUserStatus("sub-1", UserStatus.DISABLED);

        assertThat(cache.get("sub-1")).isNull();
        assertThat(cache.get("sub-2")).isNotNull();
    }

    @Test
    void assignRoleEvictsThatSubject() {
        when(users.findByOidcSubject("sub-1")).thenReturn(Optional.of(new AppUserEntity("sub-1", "m", "m@e")));
        when(roles.findByKey("hr-mgr")).thenReturn(Optional.of(new RoleEntity("hr-mgr", "HR Manager")));

        adapter.assignRole("sub-1", "hr-mgr", AccessScope.GLOBAL);

        assertThat(cache.get("sub-1")).isNull();
        assertThat(cache.get("sub-2")).isNotNull();
    }

    @Test
    void upsertRoleEvictsEntireCache() {
        when(permissions.findByKey("hr.salary:read"))
                .thenReturn(Optional.of(new PermissionEntity("hr.salary:read", "Read salary")));
        when(roles.findByKey("hr-mgr")).thenReturn(Optional.empty());

        adapter.upsertRole("hr-mgr", "HR Manager", Set.of("hr.salary:read"));

        // a role's permission set affects every holder → whole cache cleared
        assertThat(cache.get("sub-1")).isNull();
        assertThat(cache.get("sub-2")).isNull();
    }
}
