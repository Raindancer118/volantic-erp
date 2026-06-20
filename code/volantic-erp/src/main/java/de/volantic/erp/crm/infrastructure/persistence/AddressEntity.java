package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.core.AbstractEntity;
import de.volantic.erp.crm.domain.model.AddressType;
import de.volantic.erp.crm.domain.model.PartnerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/** JPA representation of a partner address. Table {@code crm.address}; owner is a flat (type, id) reference. */
@Entity
@Table(schema = "crm", name = "address")
class AddressEntity extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, updatable = false)
    private PartnerType ownerType;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AddressType type;

    @Column(name = "street", nullable = false)
    private String street;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    protected AddressEntity() {
    }

    AddressEntity(UUID id, PartnerType ownerType, UUID ownerId, AddressType type,
                  String street, String postalCode, String city, String countryCode) {
        super(id);
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.type = type;
        this.street = street;
        this.postalCode = postalCode;
        this.city = city;
        this.countryCode = countryCode;
    }

    void apply(AddressType type, String street, String postalCode, String city, String countryCode) {
        this.type = type;
        this.street = street;
        this.postalCode = postalCode;
        this.city = city;
        this.countryCode = countryCode;
    }

    /** Detached entity carrying the expected version for a version-checked merge (optimistic locking). */
    static AddressEntity forUpdate(UUID id, PartnerType ownerType, UUID ownerId, AddressType type,
                                   String street, String postalCode, String city, String countryCode, long version) {
        AddressEntity entity = new AddressEntity(id, ownerType, ownerId, type, street, postalCode, city, countryCode);
        entity.markPersisted(version);
        return entity;
    }

    PartnerType ownerType() {
        return ownerType;
    }

    UUID ownerId() {
        return ownerId;
    }

    AddressType type() {
        return type;
    }

    String street() {
        return street;
    }

    String postalCode() {
        return postalCode;
    }

    String city() {
        return city;
    }

    String countryCode() {
        return countryCode;
    }
}
