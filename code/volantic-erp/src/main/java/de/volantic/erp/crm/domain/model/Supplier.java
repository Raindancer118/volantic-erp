package de.volantic.erp.crm.domain.model;

import de.volantic.erp.core.UuidV7;

/**
 * A supplier master record — a CRM aggregate root, sibling to {@link Customer}. Kept as its own
 * aggregate rather than sharing a partner base, because the two diverge quickly (payment terms,
 * credit limits, …). Pure domain: owns identity and invariants.
 */
public final class Supplier {

    private final SupplierId id;
    private final String supplierNumber;
    private String name;
    private String email;

    private Supplier(SupplierId id, String supplierNumber, String name, String email) {
        this.id = id;
        this.supplierNumber = requireText(supplierNumber, "supplierNumber");
        this.name = requireText(name, "name");
        this.email = normalizeEmail(email);
    }

    public static Supplier create(String supplierNumber, String name, String email) {
        return new Supplier(new SupplierId(UuidV7.randomUuid()), supplierNumber, name, email);
    }

    public static Supplier reconstitute(SupplierId id, String supplierNumber, String name, String email) {
        return new Supplier(id, supplierNumber, name, email);
    }

    public void rename(String newName) {
        this.name = requireText(newName, "name");
    }

    public void changeEmail(String newEmail) {
        this.email = normalizeEmail(newEmail);
    }

    public SupplierId id() {
        return id;
    }

    public String supplierNumber() {
        return supplierNumber;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalized = email.strip().toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException("email must contain '@'");
        }
        return normalized;
    }
}
