package de.volantic.erp.crm.application;

/** Base for "business key already taken" / conflict errors in the CRM module. Mapped to HTTP 409. */
public abstract class CrmConflictException extends RuntimeException {

    protected CrmConflictException(String message) {
        super(message);
    }
}
