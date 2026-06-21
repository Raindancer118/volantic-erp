package de.volantic.erp.crm.application.port.out;

import de.volantic.erp.crm.domain.model.Address;
import de.volantic.erp.crm.domain.model.AddressId;
import de.volantic.erp.crm.domain.model.AddressType;
import de.volantic.erp.crm.domain.model.PartnerRef;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/** Outbound port for address persistence. */
public interface AddressRepository {

    Address save(Address address);

    Optional<Address> findById(AddressId id);

    Page<Address> findByOwner(PartnerRef owner, Pageable pageable);

    boolean deleteById(AddressId id);

    /** Ids of addresses matching the optional equality filter (null fields ignored, ANDed). */
    List<AddressId> findIds(AddressType type, String city, String postalCode, String countryCode);
}
