package de.volantic.erp.catalog.infrastructure.persistence;

import de.volantic.erp.catalog.domain.model.Product;
import de.volantic.erp.catalog.domain.model.ProductId;
import de.volantic.erp.core.AbstractEntity;
import de.volantic.erp.core.measure.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Currency;

/** JPA representation of a product. Table {@code catalog.product}; price stored as amount + currency. */
@Entity
@Table(schema = "catalog", name = "product")
class ProductEntity extends AbstractEntity {

    @Column(name = "sku", nullable = false, unique = true, updatable = false)
    private String sku;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal priceAmount;

    @Column(name = "price_currency", nullable = false, length = 3)
    private String priceCurrency;

    protected ProductEntity() {
    }

    private ProductEntity(Product product) {
        super(product.id().value());
        this.sku = product.sku();
        apply(product);
    }

    static ProductEntity from(Product product) {
        return new ProductEntity(product);
    }

    void apply(Product product) {
        this.name = product.name();
        this.priceAmount = product.listPrice().amount();
        this.priceCurrency = product.listPrice().currency().getCurrencyCode();
    }

    Product toDomain() {
        Money price = Money.of(priceAmount, Currency.getInstance(priceCurrency));
        return Product.reconstitute(new ProductId(getId()), sku, name, price);
    }
}
