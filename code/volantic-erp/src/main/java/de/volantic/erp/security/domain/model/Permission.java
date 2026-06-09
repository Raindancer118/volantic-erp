package de.volantic.erp.security.domain.model;

/**
 * Eine atomare Berechtigung als {@code resource:action}-Schlüssel (z. B. {@code hr.salary:read}).
 * Reines Wertobjekt — Sensible Felder bekommen eine eigene Permission, so ist Feldgenauigkeit ohne
 * Zusatzmechanik möglich (ADR-0004).
 */
public record Permission(String key) {

    public Permission {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("permission key must not be blank");
        }
    }
}
