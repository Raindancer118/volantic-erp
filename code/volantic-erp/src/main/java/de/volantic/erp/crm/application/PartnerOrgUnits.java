package de.volantic.erp.crm.application;

import de.volantic.erp.crm.application.port.out.CustomerRepository;
import de.volantic.erp.crm.application.port.out.SupplierRepository;
import de.volantic.erp.crm.domain.model.CustomerId;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.SupplierId;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves the organizational unit an address or contact inherits from its owning partner (ADR-0007):
 * an address/contact has no org unit of its own — it belongs to whichever unit owns its customer or
 * supplier. Shared by {@code AddressService} and {@code ContactService} so the inheritance rule lives in
 * one place (DRY). Reads the owner through the repository ports directly, so it resolves the scope
 * without recursively triggering the owner's own permission checks.
 */
@Component
class PartnerOrgUnits {

    private final CustomerRepository customers;
    private final SupplierRepository suppliers;

    PartnerOrgUnits(CustomerRepository customers, SupplierRepository suppliers) {
        this.customers = customers;
        this.suppliers = suppliers;
    }

    /** The org unit of the partner referenced by {@code owner}; throws if the partner does not exist. */
    UUID of(PartnerRef owner) {
        return switch (owner.type()) {
            case CUSTOMER -> customers.findById(new CustomerId(owner.id()))
                    .orElseThrow(() -> unknownOwner(owner)).orgUnitId();
            case SUPPLIER -> suppliers.findById(new SupplierId(owner.id()))
                    .orElseThrow(() -> unknownOwner(owner)).orgUnitId();
        };
    }

    private static IllegalArgumentException unknownOwner(PartnerRef owner) {
        return new IllegalArgumentException("owning " + owner.type() + " does not exist: " + owner.id());
    }
}
