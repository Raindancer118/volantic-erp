package de.volantic.erp.catalog.application;

import de.volantic.erp.catalog.application.port.out.ProductRepository;
import de.volantic.erp.catalog.domain.model.Product;
import de.volantic.erp.catalog.domain.model.ProductId;
import de.volantic.erp.core.measure.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Product use cases. Authorization enforced at the service boundary (ADR-0004). */
@Service
public class ProductService {

    private final ProductRepository products;

    ProductService(ProductRepository products) {
        this.products = products;
    }

    @Transactional
    @PreAuthorize("hasPermission(null, 'catalog.product:create')")
    public Product createProduct(String sku, String name, Money listPrice) {
        if (products.existsBySku(sku)) {
            throw new CatalogExceptions.SkuAlreadyExists(sku);
        }
        return products.save(Product.create(sku, name, listPrice));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'catalog.product:read')")
    public Product getProduct(ProductId id) {
        return products.findById(id).orElseThrow(() -> new CatalogExceptions.ProductNotFound(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'catalog.product:read')")
    public Page<Product> listProducts(Pageable pageable) {
        return products.findAll(pageable);
    }

    @Transactional
    @PreAuthorize("hasPermission(null, 'catalog.product:update')")
    public Product updateProduct(ProductId id, String name, Money listPrice) {
        Product product = products.findById(id).orElseThrow(() -> new CatalogExceptions.ProductNotFound(id));
        product.rename(name);
        product.reprice(listPrice);
        return products.save(product);
    }
}
