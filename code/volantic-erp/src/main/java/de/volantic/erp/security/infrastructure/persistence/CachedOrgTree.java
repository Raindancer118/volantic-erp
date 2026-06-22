package de.volantic.erp.security.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

/**
 * Serializable, JSON-friendly snapshot of the whole org-unit tree as {@code (id, parentId)} edges,
 * stored in Redis (cache name {@link CacheNames#ORG_TREE}). The tree is small and changes rarely, so
 * caching it keeps hierarchy resolution off the database on the authorization hot path. Ancestor and
 * descendant queries are computed in memory from these edges.
 */
public record CachedOrgTree(List<Edge> edges) {

    /** One tree edge; {@code parentId} is null for a root unit. */
    public record Edge(UUID id, UUID parentId) {
    }
}
