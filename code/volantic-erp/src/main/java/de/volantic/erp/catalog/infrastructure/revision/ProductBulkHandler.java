package de.volantic.erp.catalog.infrastructure.revision;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.catalog.application.CatalogExceptions;
import de.volantic.erp.catalog.application.ProductService;
import de.volantic.erp.catalog.domain.model.Product;
import de.volantic.erp.catalog.domain.model.ProductId;
import de.volantic.erp.core.measure.Money;
import de.volantic.erp.core.revision.AbstractFieldMapHandler;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * {@code core.revision} handler for {@link Product} (resource type {@code catalog.product}). The {@code sku}
 * is the business key and not bulk-editable; name and list price are. The price is exchanged as the amount
 * plus its ISO-4217 currency code so the {@link Money} value object round-trips exactly. Reads/writes run
 * through {@link ProductService}.
 */
@Component
class ProductBulkHandler extends AbstractFieldMapHandler {

    static final String TYPE = "catalog.product";
    static final String FIELD_NAME = "name";
    static final String FIELD_PRICE_AMOUNT = "listPriceAmount";
    static final String FIELD_PRICE_CURRENCY = "listPriceCurrency";

    private final ProductService products;

    ProductBulkHandler(ProductService products, ObjectMapper json) {
        super(json);
        this.products = products;
    }

    @Override
    public String resourceType() {
        return TYPE;
    }

    @Override
    public Set<String> editableFields() {
        return Set.of(FIELD_NAME, FIELD_PRICE_AMOUNT, FIELD_PRICE_CURRENCY);
    }

    @Override
    protected Map<String, String> readFields(UUID id) {
        Product product;
        try {
            product = products.getProduct(new ProductId(id));
        } catch (CatalogExceptions.NotFound notFound) {
            return null;
        }
        Money price = product.listPrice();
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(FIELD_NAME, product.name());
        fields.put(FIELD_PRICE_AMOUNT, price.amount().toPlainString());
        fields.put(FIELD_PRICE_CURRENCY, price.currency().getCurrencyCode());
        return fields;
    }

    @Override
    protected void writeFields(UUID id, Map<String, String> fields) {
        Money price = Money.of(fields.get(FIELD_PRICE_AMOUNT), fields.get(FIELD_PRICE_CURRENCY));
        products.updateProduct(new ProductId(id), fields.get(FIELD_NAME), price);
    }

    @Override
    public Set<String> filterableFields() {
        return Set.of(FIELD_NAME, FIELD_PRICE_CURRENCY);
    }

    @Override
    public List<UUID> selectIds(Map<String, String> filter) {
        return products.findProductIds(filter.get(FIELD_NAME), filter.get(FIELD_PRICE_CURRENCY)).stream()
                .map(ProductId::value).toList();
    }
}
