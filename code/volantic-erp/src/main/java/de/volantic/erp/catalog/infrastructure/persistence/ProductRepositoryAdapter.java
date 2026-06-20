package de.volantic.erp.catalog.infrastructure.persistence;

import de.volantic.erp.catalog.application.port.out.ProductRepository;
import de.volantic.erp.catalog.domain.model.Product;
import de.volantic.erp.catalog.domain.model.ProductId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Outbound adapter for {@link ProductRepository}: maps between domain {@link Product} and JPA. */
@Component
class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpa;

    ProductRepositoryAdapter(ProductJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Product save(Product product) {
        ProductEntity entity = jpa.findById(product.id().value()).orElseGet(() -> ProductEntity.from(product));
        entity.apply(product);
        return jpa.save(entity).toDomain();
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return jpa.findById(id.value()).map(ProductEntity::toDomain);
    }

    @Override
    public boolean existsById(ProductId id) {
        return jpa.existsById(id.value());
    }

    @Override
    public boolean existsBySku(String sku) {
        return jpa.existsBySku(sku);
    }

    @Override
    public Page<Product> findAll(Pageable pageable) {
        return jpa.findAll(pageable).map(ProductEntity::toDomain);
    }

    @Override
    public List<ProductId> findIds(String name, String currencyCode) {
        return jpa.findIdsByFilter(name, currencyCode).stream().map(ProductId::new).toList();
    }
}
