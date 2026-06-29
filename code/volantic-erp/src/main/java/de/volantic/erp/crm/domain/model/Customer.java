package de.volantic.erp.crm.domain.model;

import de.volantic.erp.core.UuidV7;

import java.util.UUID;

/**
 * A customer master record — the CRM aggregate root. Pure domain: owns its identity and invariants,
 * free of persistence and framework concerns.
 *
 * <p>The {@code customerNumber} is the human-facing business key (unique). Auto-numbering via the core
 * number ranges is a follow-up; for now it is supplied when the customer is created.
 *
 * <p>Org-unit scoping (ADR-0007): each customer belongs to exactly one organizational unit
 * ({@link #orgUnitId()}), supplied at creation and immutable thereafter. Authorization for instance
 * operations is checked against that unit's scope.
 */
public final class Customer {

    private final CustomerId id;
    private final String customerNumber;
    private final UUID orgUnitId;
    private final Long version;
    private String name;
    private String email;

    private Customer(CustomerId id, Long version, UUID orgUnitId, String customerNumber, String name, String email) {
        this.id = id;
        this.version = version;
        this.orgUnitId = requireNonNull(orgUnitId, "orgUnitId");
        this.customerNumber = requireText(customerNumber, "customerNumber");
        this.name = requireText(name, "name");
        this.email = normalizeEmail(email);
    }

    /** Creates a brand-new customer with a fresh identity (no version yet — assigned on first persist). */
    public static Customer create(UUID orgUnitId, String customerNumber, String name, String email) {
        return new Customer(new CustomerId(UuidV7.randomUuid()), null, orgUnitId, customerNumber, name, email);
    }

    /** Re-creates an existing customer from persisted state (test/legacy overload without a version). */
    public static Customer reconstitute(CustomerId id, UUID orgUnitId, String customerNumber, String name, String email) {
        return new Customer(id, null, orgUnitId, customerNumber, name, email);
    }

    /** Re-creates an existing customer including its optimistic-lock version (used by the persistence adapter). */
    public static Customer reconstitute(CustomerId id, long version, UUID orgUnitId, String customerNumber,
                                        String name, String email) {
        return new Customer(id, version, orgUnitId, customerNumber, name, email);
    }

    /** The organizational unit this customer belongs to; immutable and never {@code null} (ADR-0007). */
    public UUID orgUnitId() {
        return orgUnitId;
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

    public CustomerId id() {
        return id;
    }

    public String customerNumber() {
        return customerNumber;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    /** Identity equality: two customers are the same iff they share an id, regardless of mutable state. */
    @Override
    public boolean equals(Object other) {
        return other instanceof Customer that && id.equals(that.id);
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

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeEmail(String email) {
        return EmailAddresses.normalize(email);
    }
}
