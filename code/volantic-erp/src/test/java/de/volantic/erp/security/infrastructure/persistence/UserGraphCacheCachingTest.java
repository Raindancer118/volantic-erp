package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.security.AccessScope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code @Cacheable} wiring of {@link UserGraphCache} with an in-memory cache manager —
 * no Redis, no Docker. A second lookup of the same subject is served from the cache (the repository is
 * hit only once), and the snapshot mapping is correct.
 */
@SpringJUnitConfig(UserGraphCacheCachingTest.Config.class)
class UserGraphCacheCachingTest {

    @Configuration
    @EnableCaching
    static class Config {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheNames.USER_PERMISSIONS);
        }

        @Bean
        AppUserJpaRepository userRepository() {
            return mock(AppUserJpaRepository.class);
        }

        @Bean
        UserGraphCache userGraphCache(AppUserJpaRepository userRepository) {
            return new UserGraphCache(userRepository);
        }
    }

    @Autowired
    private UserGraphCache cache;

    @Autowired
    private AppUserJpaRepository userRepository;

    @Test
    void secondLookupIsServedFromCache() {
        AppUserEntity entity = new AppUserEntity("sub-1", "m.muster", "m@example.de");
        RoleEntity role = new RoleEntity("hr-mgr", "HR Manager");
        role.addPermission(new PermissionEntity("hr.salary:read", "Read salary"));
        entity.assignRole(role, AccessScope.GLOBAL);
        when(userRepository.findWithRolesByOidcSubject("sub-1")).thenReturn(Optional.of(entity));

        CachedUser first = cache.load("sub-1");
        CachedUser second = cache.load("sub-1");

        verify(userRepository, times(1)).findWithRolesByOidcSubject("sub-1");
        assertThat(second).isEqualTo(first);
        assertThat(first.status()).isEqualTo("ACTIVE");
        assertThat(first.assignments()).singleElement()
                .satisfies(a -> {
                    assertThat(a.roleKey()).isEqualTo("hr-mgr");
                    assertThat(a.permissionKeys()).containsExactly("hr.salary:read");
                    assertThat(a.scopeType()).isNull();
                });
    }

    @Test
    void unknownSubjectIsNegativeCachedToProtectTheDatabase() {
        when(userRepository.findWithRolesByOidcSubject("ghost")).thenReturn(Optional.empty());

        assertThat(cache.load("ghost")).isNull();
        assertThat(cache.load("ghost")).isNull();

        // null result is cached too, so repeated lookups of an unmirrored subject don't hit the DB
        verify(userRepository, times(1)).findWithRolesByOidcSubject("ghost");
    }
}
