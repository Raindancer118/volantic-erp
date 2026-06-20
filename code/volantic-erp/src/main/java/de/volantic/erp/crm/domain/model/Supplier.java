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
    private final Long version;
    private String name;
    private String email;

    private Supplier(SupplierId id, Long version, String supplierNumber, String name, String email) {
        this.id = id;
        this.version = version;
        this.supplierNumber = requireText(supplierNumber, "supplierNumber");
        this.name = requireText(name, "name");
        this.email = normalizeEmail(email);
    }

    public static Supplier create(String supplierNumber, String name, String email) {
        return new Supplier(new SupplierId(UuidV7.randomUuid()), null, supplierNumber, name, email);
    }

    public static Supplier reconstitute(SupplierId id, String supplierNumber, String name, String email) {
        return new Supplier(id, null, supplierNumber, name, email);
    }

    /** Re-creates an existing supplier including its optimistic-lock version (used by the persistence adapter). */
    public static Supplier reconstitute(SupplierId id, long version, String supplierNumber, String name, String email) {
        return new Supplier(id, version, supplierNumber, name, email);
    }

    /** Optimistic-lock version this aggregate was loaded at; {@code null} for a not-yet-persisted one. */
    public Long version() {
        return version;
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

    /** Identity equality: two suppliers are the same iff they share an id, regardless of mutable state. */
    @Override
    public boolean equals(Object other) {
        return other instanceof Supplier that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    private static String normalizeEmail(String email) {
        return EmailAddresses.normalize(email);
    }
}
