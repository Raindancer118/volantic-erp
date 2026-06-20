package de.volantic.erp.catalog.application.port.out;

import de.volantic.erp.catalog.domain.model.Product;
import de.volantic.erp.catalog.domain.model.ProductId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/** Outbound port for product persistence. */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(ProductId id);

    boolean existsById(ProductId id);

    boolean existsBySku(String sku);

    Page<Product> findAll(Pageable pageable);

    /** Ids of products matching the optional equality filter (null fields ignored, ANDed). */
    List<ProductId> findIds(String name, String currencyCode);
}
