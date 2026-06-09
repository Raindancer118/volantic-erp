package de.volantic.erp.catalog.infrastructure.persistence;

import de.volantic.erp.catalog.application.port.out.BomRepository;
import de.volantic.erp.catalog.domain.model.Bom;
import de.volantic.erp.catalog.domain.model.BomId;
import de.volantic.erp.catalog.domain.model.ProductId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Outbound adapter for {@link BomRepository}: maps between domain {@link Bom} and JPA. */
@Component
class BomRepositoryAdapter implements BomRepository {

    private final BomJpaRepository jpa;

    BomRepositoryAdapter(BomJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Bom save(Bom bom) {
        return jpa.save(BomEntity.from(bom)).toDomain();
    }

    @Override
    public Optional<Bom> findById(BomId id) {
        return jpa.findById(id.value()).map(BomEntity::toDomain);
    }

    @Override
    public boolean existsByProductIdAndVersion(ProductId productId, int version) {
        return jpa.existsByProductIdAndBomVersion(productId.value(), version);
    }

    @Override
    public List<Bom> findByProductId(ProductId productId) {
        return jpa.findByProductIdOrderByBomVersionAsc(productId.value()).stream()
                .map(BomEntity::toDomain)
                .toList();
    }
}
