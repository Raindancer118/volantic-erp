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

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email")
    private String email;

    protected CustomerEntity() {
    }

    CustomerEntity(UUID id, String customerNumber, String name, String email) {
        super(id);
        this.customerNumber = customerNumber;
        this.name = name;
        this.email = email;
    }

    /** Applies the mutable fields from the domain aggregate (customerNumber is immutable). */
    void apply(String name, String email) {
        this.name = name;
        this.email = email;
    }

    /**
     * Builds a detached entity carrying the expected optimistic-lock {@code version}, so a
     * {@code save()} becomes a version-checked merge (lost-update protection) instead of a re-load.
     */
    static CustomerEntity forUpdate(UUID id, String customerNumber, String name, String email, long version) {
        CustomerEntity entity = new CustomerEntity(id, customerNumber, name, email);
        entity.markPersisted(version);
        return entity;
    }

    String customerNumber() {
        return customerNumber;
    }

    String name() {
        return name;
    }

    String email() {
        return email;
    }
}
