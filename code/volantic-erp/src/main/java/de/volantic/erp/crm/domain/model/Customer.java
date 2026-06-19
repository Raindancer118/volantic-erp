package de.volantic.erp.crm.domain.model;

import de.volantic.erp.core.UuidV7;

/**
 * A customer master record — the CRM aggregate root. Pure domain: owns its identity and invariants,
 * free of persistence and framework concerns.
 *
 * <p>The {@code customerNumber} is the human-facing business key (unique). Auto-numbering via the core
 * number ranges is a follow-up; for now it is supplied when the customer is created.
 */
public final class Customer {

    private final CustomerId id;
    private final OrgUnitId orgUnitId;
    private final String customerNumber;
    private String name;
    private String email;

    private Customer(CustomerId id, OrgUnitId orgUnitId, String customerNumber, String name, String email) {
        this.id = id;
        this.orgUnitId = requireOrgUnit(orgUnitId);
        this.customerNumber = requireText(customerNumber, "customerNumber");
        this.name = requireText(name, "name");
        this.email = normalizeEmail(email);
    }

    /** Creates a brand-new customer with a fresh identity, belonging to the given org unit. */
    public static Customer create(OrgUnitId orgUnitId, String customerNumber, String name, String email) {
        return new Customer(new CustomerId(UuidV7.randomUuid()), orgUnitId, customerNumber, name, email);
    }

    /** Re-creates an existing customer from persisted state (used by the persistence adapter). */
    public static Customer reconstitute(CustomerId id, OrgUnitId orgUnitId,
                                        String customerNumber, String name, String email) {
        return new Customer(id, orgUnitId, customerNumber, name, email);
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

    /** The organizational unit this customer belongs to — the data scope for authorization. */
    public OrgUnitId orgUnitId() {
        return orgUnitId;
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

    private static OrgUnitId requireOrgUnit(OrgUnitId orgUnitId) {
        if (orgUnitId == null) {
            throw new IllegalArgumentException("orgUnitId must not be null");
        }
        return orgUnitId;
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
