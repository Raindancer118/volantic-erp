package de.volantic.erp.catalog.api;

import de.volantic.erp.catalog.domain.model.Product;

import java.math.BigDecimal;

/** Response body representing a product (REST v1). No JPA entity ever leaves the api layer. */
public record ProductResponse(String id, String sku, String name, BigDecimal priceAmount, String priceCurrency) {

    static ProductResponse from(Product product) {
        return new ProductResponse(
                product.id().value().toString(),
                product.sku(),
                product.name(),
                product.listPrice().amount(),
                product.listPrice().currency().getCurrencyCode());
    }
}
