package de.volantic.erp.crm.application.port.out;

import de.volantic.erp.crm.domain.model.Address;
import de.volantic.erp.crm.domain.model.AddressId;
import de.volantic.erp.crm.domain.model.PartnerRef;

import java.util.List;
import java.util.Optional;

/** Outbound port for address persistence. */
public interface AddressRepository {

    Address save(Address address);

    Optional<Address> findById(AddressId id);

    List<Address> findByOwner(PartnerRef owner);

    boolean deleteById(AddressId id);
}
