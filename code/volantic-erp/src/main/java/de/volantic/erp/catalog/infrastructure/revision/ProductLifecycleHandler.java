package de.volantic.erp.catalog.infrastructure.revision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.catalog.application.CatalogExceptions;
import de.volantic.erp.catalog.application.ProductService;
import de.volantic.erp.catalog.domain.model.Product;
import de.volantic.erp.catalog.domain.model.ProductId;
import de.volantic.erp.core.measure.Money;
import de.volantic.erp.core.revision.LifecycleResourceHandler;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@code core.revision} lifecycle handler for {@link Product} (resource type {@code catalog.product}):
 * bulk create and delete with Rollback-Engine reversal (ADR-0006 §2). The snapshot keeps the business key
 * ({@code sku}) and the list price (amount + ISO currency) so a deleted product is re-created with its
 * original id and data. All operations go through {@link ProductService}.
 */
@Component
class ProductLifecycleHandler implements LifecycleResourceHandler {

    private static final TypeReference<LinkedHashMap<String, String>> MAP = new TypeReference<>() {
    };
    private static final String SKU = "sku";
    private static final String NAME = "name";
    private static final String PRICE_AMOUNT = "listPriceAmount";
    private static final String PRICE_CURRENCY = "listPriceCurrency";

    private final ProductService products;
    private final ObjectMapper json;

    ProductLifecycleHandler(ProductService products, ObjectMapper json) {
        this.products = products;
        this.json = json;
    }

    @Override
    public String resourceType() {
        return "catalog.product";
    }

    @Override
    public UUID create(Map<String, String> data) {
        return products.createProduct(data.get(SKU), data.get(NAME), priceOf(data)).id().value();
    }

    @Override
    public String snapshot(UUID id) {
        Product product;
        try {
            product = products.getProduct(new ProductId(id));
        } catch (CatalogExceptions.NotFound notFound) {
            return null;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(SKU, product.sku());
        fields.put(NAME, product.name());
        fields.put(PRICE_AMOUNT, product.listPrice().amount().toPlainString());
        fields.put(PRICE_CURRENCY, product.listPrice().currency().getCurrencyCode());
        return serialize(fields);
    }

    @Override
    public void delete(UUID id) {
        products.deleteProduct(new ProductId(id));
    }

    @Override
    public void recreate(UUID id, String snapshot) {
        Map<String, String> data = deserialize(snapshot);
        products.recreateProduct(new ProductId(id), data.get(SKU), data.get(NAME), priceOf(data));
    }

    private static Money priceOf(Map<String, String> data) {
        return Money.of(data.get(PRICE_AMOUNT), data.get(PRICE_CURRENCY));
    }

    private String serialize(Map<String, String> fields) {
        try {
            return json.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialize product snapshot", e);
        }
    }

    private Map<String, String> deserialize(String snapshot) {
        try {
            return json.readValue(snapshot, MAP);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not deserialize product snapshot", e);
        }
    }
}
