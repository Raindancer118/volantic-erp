package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.security.application.port.out.OrgUnitHierarchy;
import de.volantic.erp.security.application.port.out.OrgUnitRepository;
import de.volantic.erp.security.domain.model.OrgUnit;
import de.volantic.erp.security.domain.model.OrgUnitId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hierarchy integration test: seeds a four-unit tree (root → childA → grandchild, root → childB) against
 * a real PostgreSQL and exercises the real {@link OrgUnitHierarchy} adapter for both ancestor and
 * descendant resolution.
 *
 * <p>The test uses a lightweight {@link ConcurrentMapCacheManager} in place of Redis so the
 * {@code @Cacheable} on {@link OrgTreeCache#load()} is active (the cache does NOT degrade to DB on every
 * call). Each test method evicts the cache in {@link #evictCache()} so the seeded tree is picked up fresh.
 *
 * <p>Skipped without a Docker daemon ({@code disabledWithoutDocker}); runs fully in CI.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OrgUnitRepositoryAdapter.class, OrgTreeCache.class, OrgUnitHierarchyAdapter.class,
        OrgUnitHierarchyIT.CacheConfig.class})
@Testcontainers(disabledWithoutDocker = true)
class OrgUnitHierarchyIT {

    /** In-memory cache replaces Redis for this slice (no Redis in @DataJpaTest). */
    @Configuration
    @EnableCaching
    static class CacheConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheNames.ORG_TREE);
        }
    }

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OrgUnitRepository repository;

    @Autowired
    private OrgUnitHierarchy hierarchy;

    @Autowired
    private CacheManager cacheManager;

    // Seeded unit ids, populated in @BeforeEach.
    private UUID rootId;
    private UUID childAId;
    private UUID childBId;
    private UUID grandchildId;

    @BeforeEach
    void seedTree() {
        // Evict the org-tree cache so the fresh tree is loaded, not a stale entry from a previous test.
        evictCache();

        OrgUnit root = repository.save(OrgUnit.create("ROOT", "Root", null));
        rootId = root.id().value();

        OrgUnit childA = repository.save(OrgUnit.create("CHILD-A", "Child A", root.id()));
        childAId = childA.id().value();

        OrgUnit childB = repository.save(OrgUnit.create("CHILD-B", "Child B", root.id()));
        childBId = childB.id().value();

        OrgUnit grandchild = repository.save(OrgUnit.create("GRANDCHILD", "Grandchild", childA.id()));
        grandchildId = grandchild.id().value();

        // Evict again after seeding so the hierarchy adapter loads the complete tree on first use.
        evictCache();
    }

    @Test
    void ancestorChainFromGrandchildIsGrandchildThenChildAThenRoot() {
        List<UUID> ancestors = hierarchy.ancestorIds(grandchildId);

        assertThat(ancestors).containsExactly(grandchildId, childAId, rootId);
    }

    @Test
    void ancestorChainFromRootIsSelfOnly() {
        List<UUID> ancestors = hierarchy.ancestorIds(rootId);

        assertThat(ancestors).containsExactly(rootId);
    }

    @Test
    void ancestorsOfNullIsEmpty() {
        assertThat(hierarchy.ancestorIds(null)).isEmpty();
    }

    @Test
    void descendantsOfRootContainsAllFourUnits() {
        Set<UUID> descendants = hierarchy.descendantIds(Set.of(rootId));

        assertThat(descendants).containsExactlyInAnyOrder(rootId, childAId, childBId, grandchildId);
    }

    @Test
    void descendantsOfChildAIsChildAAndGrandchild() {
        Set<UUID> descendants = hierarchy.descendantIds(Set.of(childAId));

        assertThat(descendants).containsExactlyInAnyOrder(childAId, grandchildId);
    }

    @Test
    void descendantsOfChildBIsChildBOnly() {
        Set<UUID> descendants = hierarchy.descendantIds(Set.of(childBId));

        assertThat(descendants).containsExactlyInAnyOrder(childBId);
    }

    @Test
    void descendantsOfLeafIsLeafOnly() {
        Set<UUID> descendants = hierarchy.descendantIds(Set.of(grandchildId));

        assertThat(descendants).containsExactlyInAnyOrder(grandchildId);
    }

    @Test
    void descendantsOfEmptySetIsEmpty() {
        assertThat(hierarchy.descendantIds(Set.of())).isEmpty();
    }

    private void evictCache() {
        var cache = cacheManager.getCache(CacheNames.ORG_TREE);
        if (cache != null) {
            cache.clear();
        }
    }
}
