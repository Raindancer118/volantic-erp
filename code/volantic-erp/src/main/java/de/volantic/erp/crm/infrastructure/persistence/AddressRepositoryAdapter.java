package de.volantic.erp.crm.infrastructure.persistence;

import de.volantic.erp.crm.application.port.out.AddressRepository;
import de.volantic.erp.crm.domain.model.Address;
import de.volantic.erp.crm.domain.model.AddressId;
import de.volantic.erp.crm.domain.model.PartnerRef;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Outbound adapter for {@link AddressRepository}: maps between domain {@link Address} and JPA. */
@Component
class AddressRepositoryAdapter implements AddressRepository {

    private final AddressJpaRepository jpa;

    AddressRepositoryAdapter(AddressJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Address save(Address address) {
        AddressEntity entity = jpa.findById(address.id().value())
                .orElseGet(() -> new AddressEntity(
                        address.id().value(), address.owner().type(), address.owner().id(), address.type(),
                        address.street(), address.postalCode(), address.city(), address.countryCode()));
        entity.apply(address.type(), address.street(), address.postalCode(), address.city(), address.countryCode());
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Address> findById(AddressId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Address> findByOwner(PartnerRef owner) {
        return jpa.findByOwnerTypeAndOwnerId(owner.type(), owner.id()).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean deleteById(AddressId id) {
        if (!jpa.existsById(id.value())) {
            return false;
        }
        jpa.deleteById(id.value());
        return true;
    }

    private Address toDomain(AddressEntity entity) {
        return Address.reconstitute(
                new AddressId(entity.getId()),
                PartnerRef.of(entity.ownerType(), entity.ownerId()),
                entity.type(), entity.street(), entity.postalCode(), entity.city(), entity.countryCode());
    }
}
