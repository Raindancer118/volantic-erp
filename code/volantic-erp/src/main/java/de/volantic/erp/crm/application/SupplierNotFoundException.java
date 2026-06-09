package de.volantic.erp.crm.application;

import de.volantic.erp.crm.domain.model.SupplierId;

/** Thrown when a supplier is requested by an id that does not exist. Mapped to HTTP 404. */
public class SupplierNotFoundException extends CrmNotFoundException {

    public SupplierNotFoundException(SupplierId id) {
        super("supplier not found: " + id.value());
    }
}
