package de.volantic.erp.crm.application;

/** Base for "entity not found" errors in the CRM module. Mapped to HTTP 404 in the api layer. */
public abstract class CrmNotFoundException extends RuntimeException {

    protected CrmNotFoundException(String message) {
        super(message);
    }
}
