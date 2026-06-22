package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.security.application.port.out.OrgUnitHierarchy;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves ancestor/descendant relationships from the cached org-unit tree ({@link OrgTreeCache}).
 * Pure in-memory graph walks over the edge snapshot — no per-call database access. Guards against a
 * malformed tree (a cycle would otherwise loop forever) by tracking visited nodes.
 */
@Component
class OrgUnitHierarchyAdapter implements OrgUnitHierarchy {

    private final OrgTreeCache cache;

    OrgUnitHierarchyAdapter(OrgTreeCache cache) {
        this.cache = cache;
    }

    @Override
    public List<UUID> ancestorIds(UUID orgUnitId) {
        if (orgUnitId == null) {
            return List.of();
        }
        // Resolve against a single tree snapshot per call (no second cache.load(), so no
        // time-of-check/time-of-use window if the tree is evicted mid-request).
        Map<UUID, UUID> parentOf = parentMap(cache.load().edges());
        if (!parentOf.containsKey(orgUnitId)) {
            return List.of(); // unknown unit
        }
        List<UUID> chain = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        UUID current = orgUnitId;
        while (current != null && seen.add(current)) {
            chain.add(current);
            current = parentOf.get(current);
        }
        return chain;
    }

    @Override
    public Set<UUID> descendantIds(Set<UUID> orgUnitIds) {
        if (orgUnitIds == null || orgUnitIds.isEmpty()) {
            return Set.of();
        }
        Map<UUID, List<UUID>> childrenOf = childrenMap(cache.load().edges());
        Set<UUID> result = new HashSet<>();
        Deque<UUID> queue = new ArrayDeque<>(orgUnitIds);
        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            if (!result.add(current)) {
                continue; // already visited — also breaks any accidental cycle
            }
            childrenOf.getOrDefault(current, List.of()).forEach(queue::add);
        }
        return result;
    }

    private static Map<UUID, UUID> parentMap(List<CachedOrgTree.Edge> edges) {
        Map<UUID, UUID> parentOf = new HashMap<>();
        for (CachedOrgTree.Edge edge : edges) {
            parentOf.put(edge.id(), edge.parentId());
        }
        return parentOf;
    }

    private static Map<UUID, List<UUID>> childrenMap(List<CachedOrgTree.Edge> edges) {
        Map<UUID, List<UUID>> childrenOf = new HashMap<>();
        for (CachedOrgTree.Edge edge : edges) {
            if (edge.parentId() != null) {
                childrenOf.computeIfAbsent(edge.parentId(), k -> new ArrayList<>()).add(edge.id());
            }
        }
        return childrenOf;
    }
}
