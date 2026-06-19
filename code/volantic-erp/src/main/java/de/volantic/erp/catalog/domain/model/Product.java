package de.volantic.erp.catalog.domain.model;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.core.measure.Money;

/**
 * A product / article — the catalog aggregate root. The {@code sku} (stock keeping unit) is the
 * human-facing business key; {@code listPrice} is the catalog price. Pure domain.
 */
public final class Product {

    private final ProductId id;
    private final String sku;
    private String name;
    private Money listPrice;

    private Product(ProductId id, String sku, String name, Money listPrice) {
        this.id = id;
        this.sku = requireText(sku, "sku");
        this.name = requireText(name, "name");
        this.listPrice = requireNonNull(listPrice);
    }

    public static Product create(String sku, String name, Money listPrice) {
        return new Product(new ProductId(UuidV7.randomUuid()), sku, name, listPrice);
    }

    public static Product reconstitute(ProductId id, String sku, String name, Money listPrice) {
        return new Product(id, sku, name, listPrice);
    }

    public void rename(String newName) {
        this.name = requireText(newName, "name");
    }

    public void reprice(Money newPrice) {
        this.listPrice = requireNonNull(newPrice);
    }

    public ProductId id() {
        return id;
    }

    public String sku() {
        return sku;
    }

    public String name() {
        return name;
    }

    public Money listPrice() {
        return listPrice;
    }

    /** Identity equality: two products are the same iff they share an id, regardless of mutable state. */
    @Override
    public boolean equals(Object other) {
        return other instanceof Product that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    private static Money requireNonNull(Money price) {
        if (price == null) {
            throw new IllegalArgumentException("listPrice must not be null");
        }
        if (price.isNegative()) {
            throw new IllegalArgumentException("listPrice must not be negative");
        }
        return price;
    }
}
