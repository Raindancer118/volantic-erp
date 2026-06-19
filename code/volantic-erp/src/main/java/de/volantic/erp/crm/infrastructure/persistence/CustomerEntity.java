package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.core.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/** JPA representation of a customer. Table {@code crm.customer}; the id is assigned by the domain. */
@Entity
@Table(schema = "crm", name = "customer")
class CustomerEntity extends AbstractEntity {

    @Column(name = "customer_number", nullable = false, unique = true, updatable = false)
    private String customerNumber;

    @Column(name = "org_unit_id", nullable = false, updatable = false)
    private UUID orgUnitId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email")
    private String email;

    protected CustomerEntity() {
    }

    CustomerEntity(UUID id, UUID orgUnitId, String customerNumber, String name, String email) {
        super(id);
        this.orgUnitId = orgUnitId;
        this.customerNumber = customerNumber;
        this.name = name;
        this.email = email;
    }

    /** Applies the mutable fields from the domain aggregate (customerNumber is immutable). */
    void apply(String name, String email) {
        this.name = name;
        this.email = email;
    }

    String customerNumber() {
        return customerNumber;
    }

    UUID orgUnitId() {
        return orgUnitId;
    }

    String name() {
        return name;
    }

    String email() {
        return email;
    }
}
