package de.volantic.erp.security.application;

import de.volantic.erp.security.domain.model.OrgUnitId;

/** An org unit (or a referenced parent unit) was not found. Mapped to HTTP 404 in the api layer. */
public class OrgUnitNotFoundException extends RuntimeException {

    public OrgUnitNotFoundException(OrgUnitId id) {
        super("org unit not found: " + id.value());
    }
}
