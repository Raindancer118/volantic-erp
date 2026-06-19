package de.volantic.erp.crm.domain.model;

import de.volantic.erp.core.UuidV7;

import java.util.Locale;
import java.util.Set;

/**
 * A postal address belonging to a business partner (customer or supplier). Small aggregate referencing
 * its owner via {@link PartnerRef}. Pure domain.
 */
public final class Address {

    /** ISO 3166-1 alpha-2 codes known to the JVM, used to reject made-up codes like {@code XX}. */
    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

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

    /** Identity equality: two addresses are the same iff they share an id, regardless of mutable state. */
    @Override
    public boolean equals(Object other) {
        return other instanceof Address that && id.equals(that.id);
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

    private static String normalizeCountry(String countryCode) {
        String value = requireText(countryCode, "countryCode").toUpperCase();
        if (!ISO_COUNTRIES.contains(value)) {
            throw new IllegalArgumentException("countryCode must be a valid ISO 3166-1 alpha-2 code: " + value);
        }
        return value;
    }
}
