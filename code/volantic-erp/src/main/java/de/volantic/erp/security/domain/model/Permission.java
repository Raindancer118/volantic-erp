package de.volantic.erp.security.domain.model;

/**
 * An atomic permission as a {@code resource:action} key (e.g. {@code hr.salary:read}). Pure value
 * object — sensitive fields get their own permission, so field-level granularity is possible without
 * extra machinery (ADR-0004).
 */
public record Permission(String key) {

    public Permission {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("permission key must not be blank");
        }
    }
}
