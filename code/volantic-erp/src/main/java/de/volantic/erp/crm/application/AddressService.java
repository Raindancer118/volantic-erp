package de.volantic.erp.crm.application;

import de.volantic.erp.crm.application.port.out.AddressRepository;
import de.volantic.erp.crm.domain.event.PartnerAddressLinked;
import de.volantic.erp.crm.domain.event.PartnerAddressUnlinked;
import de.volantic.erp.crm.domain.model.Address;
import de.volantic.erp.crm.domain.model.AddressId;
import de.volantic.erp.crm.domain.model.AddressType;
import de.volantic.erp.crm.domain.model.PartnerRef;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Address use cases. Authorization enforced here at the service boundary (ADR-0004). */
@Service
public class AddressService {

    private final AddressRepository addresses;
    private final ApplicationEventPublisher events;

    AddressService(AddressRepository addresses, ApplicationEventPublisher events) {
        this.addresses = addresses;
        this.events = events;
    }

    @Transactional
    @PreAuthorize("hasPermission(null, 'crm.address:write')")
    public Address createAddress(PartnerRef owner, AddressType type,
                                 String street, String postalCode, String city, String countryCode) {
        Address address = addresses.save(Address.create(owner, type, street, postalCode, city, countryCode));
        events.publishEvent(new PartnerAddressLinked(owner, address.id()));
        return address;
    }

    // noRollbackFor: a not-found read must not poison a surrounding transaction (see CustomerService).
    @Transactional(readOnly = true, noRollbackFor = AddressNotFoundException.class)
    @PreAuthorize("hasPermission(null, 'crm.address:read')")
    public Address getAddress(AddressId id) {
        return addresses.findById(id).orElseThrow(() -> new AddressNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'crm.address:read')")
    public Page<Address> listAddresses(PartnerRef owner, Pageable pageable) {
        return addresses.findByOwner(owner, pageable);
    }

    /** Resolves a selection filter (type/city/postalCode/countryCode, exact match) to matching ids. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'crm.address:read')")
    public List<AddressId> findAddressIds(AddressType type, String city, String postalCode, String countryCode) {
        return addresses.findIds(type, city, postalCode, countryCode);
    }

    /** Update with an explicit optimistic-lock check (REST CRUD via ETag/If-Match). */
    @Transactional
    @PreAuthorize("hasPermission(null, 'crm.address:write')")
    public Address updateAddress(AddressId id, long expectedVersion, AddressType type,
                                 String street, String postalCode, String city, String countryCode) {
        Address address = addresses.findById(id).orElseThrow(() -> new AddressNotFoundException(id));
        de.volantic.erp.core.OptimisticLock.check(address.version(), expectedVersion, id);
        address.change(type, street, postalCode, city, countryCode);
        return addresses.save(address);
    }

    /** Update without an explicit version — internal/bulk callers; still version-safe within the tx. */
    @Transactional
    @PreAuthorize("hasPermission(null, 'crm.address:write')")
    public Address updateAddress(AddressId id, AddressType type,
                                 String street, String postalCode, String city, String countryCode) {
        Address address = addresses.findById(id).orElseThrow(() -> new AddressNotFoundException(id));
        address.change(type, street, postalCode, city, countryCode);
        return addresses.save(address);
    }

    @Transactional
    @PreAuthorize("hasPermission(null, 'crm.address:write')")
    public void deleteAddress(AddressId id) {
        PartnerRef owner = addresses.findById(id).orElseThrow(() -> new AddressNotFoundException(id)).owner();
        if (!addresses.deleteById(id)) {
            throw new AddressNotFoundException(id);
        }
        events.publishEvent(new PartnerAddressUnlinked(owner, id));
    }
}
