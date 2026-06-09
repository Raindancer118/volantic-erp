package de.volantic.erp.crm.api;

import de.volantic.erp.crm.domain.model.Supplier;

/** Response body representing a supplier (REST v1). */
public record SupplierResponse(String id, String supplierNumber, String name, String email) {

    static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(
                supplier.id().value().toString(),
                supplier.supplierNumber(),
                supplier.name(),
                supplier.email());
    }
}
