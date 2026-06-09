package de.volantic.erp.crm.domain.model;

import de.volantic.erp.core.UuidV7;

/**
 * A postal address belonging to a business partner (customer or supplier). Small aggregate referencing
 * its owner via {@link PartnerRef}. Pure domain.
 */
public final class Address {

    private final AddressId id;
    private final PartnerRef owner;
    private AddressType type;
    private String street;
    private String postalCode;
    private String city;
    private String countryCode;

    private Address(AddressId id, PartnerRef owner, AddressType type,
                    String street, String postalCode, String city, String countryCode) {
        this.id = id;
        this.owner = owner;
        this.type = type == null ? AddressType.DEFAULT : type;
        this.street = requireText(street, "street");
        this.postalCode = requireText(postalCode, "postalCode");
        this.city = requireText(city, "city");
        this.countryCode = normalizeCountry(countryCode);
    }

    public static Address create(PartnerRef owner, AddressType type,
                                 String street, String postalCode, String city, String countryCode) {
        return new Address(new AddressId(UuidV7.randomUuid()), owner, type, street, postalCode, city, countryCode);
    }

    public static Address reconstitute(AddressId id, PartnerRef owner, AddressType type,
                                       String street, String postalCode, String city, String countryCode) {
        return new Address(id, owner, type, street, postalCode, city, countryCode);
    }

    public void change(AddressType type, String street, String postalCode, String city, String countryCode) {
        this.type = type == null ? AddressType.DEFAULT : type;
        this.street = requireText(street, "street");
        this.postalCode = requireText(postalCode, "postalCode");
        this.city = requireText(city, "city");
        this.countryCode = normalizeCountry(countryCode);
    }

    public AddressId id() {
        return id;
    }

    public PartnerRef owner() {
        return owner;
    }

    public AddressType type() {
        return type;
    }

    public String street() {
        return street;
    }

    public String postalCode() {
        return postalCode;
    }

    public String city() {
        return city;
    }

    public String countryCode() {
        return countryCode;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    private static String normalizeCountry(String countryCode) {
        String value = requireText(countryCode, "countryCode").toUpperCase();
        if (value.length() != 2) {
            throw new IllegalArgumentException("countryCode must be an ISO 3166-1 alpha-2 code");
        }
        return value;
    }
}
