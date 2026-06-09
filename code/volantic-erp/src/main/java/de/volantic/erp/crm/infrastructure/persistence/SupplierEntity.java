package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.core.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/** JPA representation of a supplier. Table {@code crm.supplier}; the id is assigned by the domain. */
@Entity
@Table(schema = "crm", name = "supplier")
class SupplierEntity extends AbstractEntity {

    @Column(name = "supplier_number", nullable = false, unique = true, updatable = false)
    private String supplierNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email")
    private String email;

    protected SupplierEntity() {
    }

    SupplierEntity(UUID id, String supplierNumber, String name, String email) {
        super(id);
        this.supplierNumber = supplierNumber;
        this.name = name;
        this.email = email;
    }

    void apply(String name, String email) {
        this.name = name;
        this.email = email;
    }

    String supplierNumber() {
        return supplierNumber;
    }

    String name() {
        return name;
    }

    String email() {
        return email;
    }
}
