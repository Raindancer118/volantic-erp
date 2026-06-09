package de.volantic.erp.crm.application;

/** Thrown when creating a customer with a {@code customerNumber} that is already taken. Mapped to HTTP 409. */
public class CustomerNumberAlreadyExistsException extends CrmConflictException {

    public CustomerNumberAlreadyExistsException(String customerNumber) {
        super("customer number already exists: " + customerNumber);
    }
}
