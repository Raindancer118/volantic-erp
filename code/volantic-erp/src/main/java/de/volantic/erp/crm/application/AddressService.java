package de.volantic.erp.crm.application;

import de.volantic.erp.crm.application.port.out.AddressRepository;
import de.volantic.erp.crm.domain.model.Address;
import de.volantic.erp.crm.domain.model.AddressId;
import de.volantic.erp.crm.domain.model.AddressType;
import de.volantic.erp.crm.domain.model.PartnerRef;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Address use cases. Authorization enforced here at the service boundary (ADR-0004). */
@Service
public class AddressService {

    private final AddressRepository addresses;

    AddressService(AddressRepository addresses) {
        this.addresses = addresses;
    }

    @Transactional
    @PreAuthorize("hasPermission(null, 'crm.address:write')")
    public Address createAddress(PartnerRef owner, AddressType type,
                                 String street, String postalCode, String city, String countryCode) {
        return addresses.save(Address.create(owner, type, street, postalCode, city, countryCode));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'crm.address:read')")
    public Address getAddress(AddressId id) {
        return addresses.findById(id).orElseThrow(() -> new AddressNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'crm.address:read')")
    public List<Address> listAddresses(PartnerRef owner) {
        return addresses.findByOwner(owner);
    }

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
        if (!addresses.deleteById(id)) {
            throw new AddressNotFoundException(id);
        }
    }
}
