package de.volantic.erp.changeset.application;

import java.util.List;
import java.util.Map;

/**
 * A mass-create request: create one resource of {@code resourceType} per field map in {@code records}
 * (ADR-0006 §2). Runs only in a LIVE session; the Rollback Engine takes each created resource back by
 * deleting it.
 *
 * @param resourceType module-qualified type, e.g. {@code "crm.contact"}
 * @param records      one field map (field name → value) per resource to create
 */
public record BulkCreate(String resourceType, List<Map<String, String>> records) {

    public BulkCreate {
        if (resourceType == null || resourceType.isBlank()) {
            throw new IllegalArgumentException("resourceType must not be blank");
        }
        records = records == null ? List.of() : List.copyOf(records.stream().map(Map::copyOf).toList());
    }
}
