package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.application.port.out.SecurityWriteStore;
import de.volantic.erp.security.domain.model.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Outbound adapter for {@link SecurityWriteStore}: persists users/roles/permissions via JPA and
 * invalidates the authorization cache ({@link CacheNames#USER_PERMISSIONS}).
 *
 * <p>Eviction runs <strong>after commit</strong> (via {@link TransactionSynchronization}), not before:
 * evicting before commit could let a concurrent read repopulate the cache with the still-uncommitted
 * old value, leaving it stale until the TTL. After-commit eviction avoids that window. When there is no
 * active transaction (e.g. tests) it evicts immediately. The {@link CacheManager} is optional so this
 * adapter also works in slices without a configured cache.
 *
 * <p>Eviction is <strong>fail-open</strong>, matching {@code CacheConfig}'s {@code CacheErrorHandler}:
 * this is a manual cache access (not annotation-driven), so it does not go through that handler and must
 * swallow cache/Redis errors itself. A Redis outage must never fail a security write — the entry's short
 * TTL bounds the resulting staleness until the cache is reachable again.
 */
@Component
class SecurityWriteStoreAdapter implements SecurityWriteStore {

    private static final Logger log = LoggerFactory.getLogger(SecurityWriteStoreAdapter.class);

    private final AppUserJpaRepository users;
    private final RoleJpaRepository roles;
    private final PermissionJpaRepository permissions;
    private final ObjectProvider<CacheManager> cacheManager;

    SecurityWriteStoreAdapter(AppUserJpaRepository users,
                              RoleJpaRepository roles,
                              PermissionJpaRepository permissions,
                              ObjectProvider<CacheManager> cacheManager) {
        this.users = users;
        this.roles = roles;
        this.permissions = permissions;
        this.cacheManager = cacheManager;
    }

    @Override
    public void upsertPermission(String key, String description) {
        if (permissions.findByKey(key).isEmpty()) {
            permissions.save(new PermissionEntity(key, description));
        }
    }

    @Override
    public void upsertRole(String roleKey, String name, Set<String> permissionKeys) {
        Set<PermissionEntity> resolved = permissionKeys.stream()
                .map(key -> permissions.findByKey(key)
                        .orElseThrow(() -> new IllegalArgumentException("unknown permission: " + key)))
                .collect(Collectors.toSet());

        RoleEntity role = roles.findByKey(roleKey).orElseGet(() -> new RoleEntity(roleKey, name));
        role.rename(name);
        role.replacePermissions(resolved);
        roles.save(role);

        // A role's permission set affects every user holding it; evict the whole cache.
        runAfterCommit(() -> withCache(Cache::clear));
    }

    @Override
    public boolean userExists(String oidcSubject) {
        return users.findByOidcSubject(oidcSubject).isPresent();
    }

    @Override
    public void createUser(String oidcSubject, String username, String email) {
        users.save(new AppUserEntity(oidcSubject, username, email));
        evictUser(oidcSubject); // clears a possible negative-cache entry from before provisioning
    }

    @Override
    public void setUserStatus(String oidcSubject, UserStatus status) {
        AppUserEntity user = requireUser(oidcSubject);
        user.changeStatus(status);
        users.save(user);
        evictUser(oidcSubject);
    }

    @Override
    public void assignRole(String oidcSubject, String roleKey, AccessScope scope) {
        AppUserEntity user = requireUser(oidcSubject);
        RoleEntity role = roles.findByKey(roleKey)
                .orElseThrow(() -> new IllegalArgumentException("unknown role: " + roleKey));
        user.assignRole(role, scope);
        users.save(user);
        evictUser(oidcSubject);
    }

    private AppUserEntity requireUser(String oidcSubject) {
        return users.findByOidcSubject(oidcSubject)
                .orElseThrow(() -> new IllegalArgumentException("unknown user: " + oidcSubject));
    }

    private void evictUser(String oidcSubject) {
        runAfterCommit(() -> withCache(cache -> cache.evictIfPresent(oidcSubject)));
    }

    private void withCache(java.util.function.Consumer<Cache> action) {
        CacheManager manager = cacheManager.getIfAvailable();
        if (manager == null) {
            return;
        }
        Cache cache = manager.getCache(CacheNames.USER_PERMISSIONS);
        if (cache == null) {
            return;
        }
        try {
            action.accept(cache);
        } catch (RuntimeException cacheError) {
            // Fail-open: a Redis outage must not fail the (already committed) security write. The short
            // TTL on USER_PERMISSIONS bounds staleness until the cache is reachable again.
            log.warn("authorization cache eviction failed [{}] — degrading to TTL: {}",
                    CacheNames.USER_PERMISSIONS, cacheError.toString());
        }
    }

    private static void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
