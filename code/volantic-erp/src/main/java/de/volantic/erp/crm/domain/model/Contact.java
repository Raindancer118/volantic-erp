package de.volantic.erp.crm.domain.model;

import de.volantic.erp.core.UuidV7;

/**
 * A contact person belonging to a business partner (customer or supplier). Small aggregate referencing
 * its owner via {@link PartnerRef}. Pure domain.
 */
public final class Contact {

    private final ContactId id;
    private final PartnerRef owner;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    private Contact(ContactId id, PartnerRef owner, String firstName, String lastName, String email, String phone) {
        this.id = id;
        this.owner = owner;
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.email = normalizeEmail(email);
        this.phone = blankToNull(phone);
    }

    public static Contact create(PartnerRef owner, String firstName, String lastName, String email, String phone) {
        return new Contact(new ContactId(UuidV7.randomUuid()), owner, firstName, lastName, email, phone);
    }

    public static Contact reconstitute(ContactId id, PartnerRef owner,
                                       String firstName, String lastName, String email, String phone) {
        return new Contact(id, owner, firstName, lastName, email, phone);
    }

    public void change(String firstName, String lastName, String email, String phone) {
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.email = normalizeEmail(email);
        this.phone = blankToNull(phone);
    }

    public ContactId id() {
        return id;
    }

    public PartnerRef owner() {
        return owner;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public String email() {
        return email;
    }

    public String phone() {
        return phone;
    }

    /** Identity equality: two contacts are the same iff they share an id, regardless of mutable state. */
    @Override
    public boolean equals(Object other) {
        return other instanceof Contact that && id.equals(that.id);
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String normalizeEmail(String email) {
        return EmailAddresses.normalize(email);
    }
}
