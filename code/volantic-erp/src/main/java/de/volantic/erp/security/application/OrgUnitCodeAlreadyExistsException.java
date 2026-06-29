package de.volantic.erp.security.application;

/** The org-unit code (business key) is already taken. Mapped to HTTP 409 in the api layer. */
public class OrgUnitCodeAlreadyExistsException extends RuntimeException {

    public OrgUnitCodeAlreadyExistsException(String code) {
        super("org unit code already exists: " + code);
    }
}
