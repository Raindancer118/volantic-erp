package de.volantic.erp.workflow.domain.model;

/**
 * What an approval is about: a flat {@code (type, id)} reference to the business object being signed off
 * (e.g. {@code ("purchase-order", "…uuid…")}). Kept as free-form strings so any module can route its
 * objects through the generic approval process without the workflow module depending on it.
 */
public record ApprovalSubject(String type, String id) {

    public ApprovalSubject {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("subject type must not be blank");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("subject id must not be blank");
        }
        type = type.strip();
        id = id.strip();
    }
}
