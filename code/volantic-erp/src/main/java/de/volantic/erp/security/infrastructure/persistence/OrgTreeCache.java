package de.volantic.erp.security.infrastructure.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Loads and caches the org-unit tree ({@link CachedOrgTree}) in Redis (cache {@link CacheNames#ORG_TREE}).
 * On a cache hit the database is not touched, keeping hierarchy resolution within the sub-500 ms budget
 * even when many scope checks run in a loop (bulk edits).
 *
 * <p>Eviction mirrors the user-graph cache ({@code SecurityWriteStoreAdapter}): the whole entry is
 * cleared <strong>after commit</strong> of any org-unit write, and is <strong>fail-open</strong> (a
 * manual cache access, so it bypasses the {@code CacheErrorHandler} and must swallow Redis errors itself
 * — a cache outage must never fail a committed write; the short TTL bounds staleness).
 */
@Component
class OrgTreeCache {

    private static final Logger log = LoggerFactory.getLogger(OrgTreeCache.class);

    private final OrgUnitJpaRepository orgUnits;
    private final ObjectProvider<CacheManager> cacheManager;

    OrgTreeCache(OrgUnitJpaRepository orgUnits, ObjectProvider<CacheManager> cacheManager) {
        this.orgUnits = orgUnits;
        this.cacheManager = cacheManager;
    }

    @Cacheable(cacheNames = CacheNames.ORG_TREE, key = "'all'")
    public CachedOrgTree load() {
        List<CachedOrgTree.Edge> edges = orgUnits.findAllEdges().stream()
                .map(e -> new CachedOrgTree.Edge(e.getId(), e.getParentId()))
                .toList();
        return new CachedOrgTree(edges);
    }

    /** Clears the cached tree after the current transaction commits (fail-open). */
    void evictAfterCommit() {
        runAfterCommit(() -> {
            CacheManager manager = cacheManager.getIfAvailable();
            if (manager == null) {
                return;
            }
            Cache cache = manager.getCache(CacheNames.ORG_TREE);
            if (cache == null) {
                return;
            }
            try {
                cache.clear();
            } catch (RuntimeException cacheError) {
                log.warn("org-tree cache eviction failed [{}] — degrading to TTL: {}",
                        CacheNames.ORG_TREE, cacheError.toString());
            }
        });
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
