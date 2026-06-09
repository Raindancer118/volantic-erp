package de.volantic.erp.crm.application;

/** Thrown when creating a supplier with a {@code supplierNumber} that is already taken. Mapped to HTTP 409. */
public class SupplierNumberAlreadyExistsException extends CrmConflictException {

    public SupplierNumberAlreadyExistsException(String supplierNumber) {
        super("supplier number already exists: " + supplierNumber);
    }
}
