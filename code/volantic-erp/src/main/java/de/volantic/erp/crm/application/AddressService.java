package de.volantic.erp.crm.application;

import de.volantic.erp.crm.application.port.out.AddressRepository;
import de.volantic.erp.crm.domain.event.PartnerAddressLinked;
import de.volantic.erp.crm.domain.event.PartnerAddressUnlinked;
import de.volantic.erp.crm.domain.model.Address;
import de.volantic.erp.crm.domain.model.AddressId;
import de.volantic.erp.crm.domain.model.AddressType;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.security.ScopeEnforcer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Address use cases. Authorization is enforced here at the service boundary (ADR-0004) with org-unit
 * scoping (ADR-0007): an address has no org unit of its own — it <em>inherits</em> the unit of its
 * owning partner ({@link PartnerOrgUnits}). Every operation resolves the owner's unit and checks it via
 * {@link ScopeEnforcer}. A global grant still covers every unit. The bulk/change-set path calls these
 * same methods, so it inherits the checks.
 */
@Service
public class AddressService {

    private static final String PERM_READ  = "crm.address:read";
    private static final String PERM_WRITE = "crm.address:write";

    private final AddressRepository addresses;
    private final ApplicationEventPublisher events;
    private final PartnerOrgUnits partnerOrgUnits;
    private final ScopeEnforcer scopeEnforcer;

    AddressService(AddressRepository addresses, ApplicationEventPublisher events,
                   PartnerOrgUnits partnerOrgUnits, ScopeEnforcer scopeEnforcer) {
        this.addresses = addresses;
        this.events = events;
        this.partnerOrgUnits = partnerOrgUnits;
        this.scopeEnforcer = scopeEnforcer;
    }

    @Transactional
    public Address createAddress(PartnerRef owner, AddressType type,
                                 String street, String postalCode, String city, String countryCode) {
        scopeEnforcer.require(PERM_WRITE, partnerOrgUnits.of(owner));
        Address address = addresses.save(Address.create(owner, type, street, postalCode, city, countryCode));
        events.publishEvent(new PartnerAddressLinked(owner, address.id()));
        return address;
    }

    // noRollbackFor: a not-found read must not poison a surrounding transaction (see CustomerService).
    @Transactional(readOnly = true, noRollbackFor = AddressNotFoundException.class)
    public Address getAddress(AddressId id) {
        Address address = addresses.findById(id).orElseThrow(() -> new AddressNotFoundException(id));
        scopeEnforcer.require(PERM_READ, partnerOrgUnits.of(address.owner()));
        return address;
    }

    @Transactional(readOnly = true)
    public Page<Address> listAddresses(PartnerRef owner, Pageable pageable) {
        scopeEnforcer.require(PERM_READ, partnerOrgUnits.of(owner));
        return addresses.findByOwner(owner, pageable);
    }

    /**
     * Resolves a selection filter (type/city/postalCode/countryCode, exact match) to matching ids — the
     * bulk-edit selection path. Per-instance scope is enforced when each selected address is updated.
     */
    @Transactional(readOnly = true)
    public List<AddressId> findAddressIds(AddressType type, String city, String postalCode, String countryCode) {
        scopeEnforcer.requireAnywhere(PERM_READ);
        return addresses.findIds(type, city, postalCode, countryCode);
    }

    /** Update with an explicit optimistic-lock check (REST CRUD via ETag/If-Match). */
    @Transactional
    public Address updateAddress(AddressId id, long expectedVersion, AddressType type,
                                 String street, String postalCode, String city, String countryCode) {
        Address address = addresses.findById(id).orElseThrow(() -> new AddressNotFoundException(id));
        scopeEnforcer.require(PERM_WRITE, partnerOrgUnits.of(address.owner()));
        de.volantic.erp.core.OptimisticLock.check(address.version(), expectedVersion, id);
        address.change(type, street, postalCode, city, countryCode);
        return addresses.save(address);
    }

    /** Update without an explicit version — internal/bulk callers; still version-safe within the tx. */
    @Transactional
    public Address updateAddress(AddressId id, AddressType type,
                                 String street, String postalCode, String city, String countryCode) {
        Address address = addresses.findById(id).orElseThrow(() -> new AddressNotFoundException(id));
        scopeEnforcer.require(PERM_WRITE, partnerOrgUnits.of(address.owner()));
        address.change(type, street, postalCode, city, countryCode);
        return addresses.save(address);
    }

    /**
     * Re-creates a previously deleted address with its original id (Rollback Engine compensation of a
     * DELETE). Re-publishes the link event so the 360° graph edge is restored.
     */
    @Transactional
    public Address recreateAddress(AddressId id, PartnerRef owner, AddressType type,
                                   String street, String postalCode, String city, String countryCode) {
        scopeEnforcer.require(PERM_WRITE, partnerOrgUnits.of(owner));
        Address address = addresses.save(Address.reconstitute(id, owner, type, street, postalCode, city, countryCode));
        events.publishEvent(new PartnerAddressLinked(owner, address.id()));
        return address;
    }

    @Transactional
    public void deleteAddress(AddressId id) {
        PartnerRef owner = addresses.findById(id).orElseThrow(() -> new AddressNotFoundException(id)).owner();
        scopeEnforcer.require(PERM_WRITE, partnerOrgUnits.of(owner));
        if (!addresses.deleteById(id)) {
            throw new AddressNotFoundException(id);
        }
        events.publishEvent(new PartnerAddressUnlinked(owner, id));
    }
}
