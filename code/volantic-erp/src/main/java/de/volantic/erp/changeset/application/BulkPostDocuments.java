package de.volantic.erp.changeset.application;

import java.util.List;
import java.util.UUID;

/**
 * A mass-posting request: post the given draft documents of one {@code resourceType} (ADR-0006 §2). Runs
 * only in a LIVE session (posting draws real gap-free numbers and cannot be buffered). The Rollback Engine
 * takes each posting back with a storno (a cancellation document in the same number range) — never a
 * delete, so the GoBD number/audit chain stays intact.
 *
 * @param resourceType module-qualified document type, e.g. {@code "sales.invoice"}
 * @param ids          the draft documents to post
 */
public record BulkPostDocuments(String resourceType, List<UUID> ids) {

    public BulkPostDocuments {
        if (resourceType == null || resourceType.isBlank()) {
            throw new IllegalArgumentException("resourceType must not be blank");
        }
        ids = ids == null ? List.of() : List.copyOf(ids);
    }
}
