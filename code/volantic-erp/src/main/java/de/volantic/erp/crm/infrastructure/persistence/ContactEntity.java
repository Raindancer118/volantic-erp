package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.core.AbstractEntity;
import de.volantic.erp.crm.domain.model.PartnerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/** JPA representation of a partner contact. Table {@code crm.contact}; owner is a flat (type, id) reference. */
@Entity
@Table(schema = "crm", name = "contact")
class ContactEntity extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, updatable = false)
    private PartnerType ownerType;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    protected ContactEntity() {
    }

    ContactEntity(UUID id, PartnerType ownerType, UUID ownerId,
                  String firstName, String lastName, String email, String phone) {
        super(id);
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

    void apply(String firstName, String lastName, String email, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

    PartnerType ownerType() {
        return ownerType;
    }

    UUID ownerId() {
        return ownerId;
    }

    String firstName() {
        return firstName;
    }

    String lastName() {
        return lastName;
    }

    String email() {
        return email;
    }

    String phone() {
        return phone;
    }
}
