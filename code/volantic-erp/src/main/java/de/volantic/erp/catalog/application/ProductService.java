package de.volantic.erp.catalog.application;

import de.volantic.erp.catalog.application.port.out.ProductRepository;
import de.volantic.erp.catalog.domain.model.Product;
import de.volantic.erp.catalog.domain.model.ProductId;
import de.volantic.erp.core.OptimisticLock;
import de.volantic.erp.core.measure.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    // noRollbackFor: a not-found read must not poison a surrounding transaction (see CustomerService) —
    // the change-set ProductBulkHandler probes existence inside a wider tx via getProduct.
    @Transactional(readOnly = true, noRollbackFor = CatalogExceptions.NotFound.class)
    @PreAuthorize("hasPermission(null, 'catalog.product:read')")
    public Product getProduct(ProductId id) {
        return products.findById(id).orElseThrow(() -> new CatalogExceptions.ProductNotFound(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'catalog.product:read')")
    public Page<Product> listProducts(Pageable pageable) {
        return products.findAll(pageable);
    }

    /** Resolves a selection filter (name and/or ISO currency code, exact match) to matching product ids. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'catalog.product:read')")
    public List<ProductId> findProductIds(String name, String currencyCode) {
        return products.findIds(name, currencyCode);
    }

    /** Update with an explicit optimistic-lock check (REST CRUD via ETag/If-Match). */
    @Transactional
    @PreAuthorize("hasPermission(null, 'catalog.product:update')")
    public Product updateProduct(ProductId id, long expectedVersion, String name, Money listPrice) {
        Product product = products.findById(id).orElseThrow(() -> new CatalogExceptions.ProductNotFound(id));
        OptimisticLock.check(product.version(), expectedVersion, id);
        product.rename(name);
        product.reprice(listPrice);
        return products.save(product);
    }

    /** Update without an explicit version — internal/bulk callers; still version-safe within the tx. */
    @Transactional
    @PreAuthorize("hasPermission(null, 'catalog.product:update')")
    public Product updateProduct(ProductId id, String name, Money listPrice) {
        Product product = products.findById(id).orElseThrow(() -> new CatalogExceptions.ProductNotFound(id));
        product.rename(name);
        product.reprice(listPrice);
        return products.save(product);
    }
}
