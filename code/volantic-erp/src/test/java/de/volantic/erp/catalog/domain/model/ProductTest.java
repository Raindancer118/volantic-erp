package de.volantic.erp.catalog.domain.model;

import de.volantic.erp.core.measure.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Invariants of the {@link Product} aggregate root (pure domain, no Spring). */
class ProductTest {

    @Test
    void createTrimsAndKeepsValues() {
        Product product = Product.create("  SKU-1 ", "  Widget ", Money.of("19.99", "EUR"));

        assertThat(product.id()).isNotNull();
        assertThat(product.sku()).isEqualTo("SKU-1");
        assertThat(product.name()).isEqualTo("Widget");
        assertThat(product.listPrice()).isEqualTo(Money.of("19.99", "EUR"));
    }

    @Test
    void renameAndRepriceMutate() {
        Product product = Product.create("SKU-1", "Widget", Money.of("10.00", "EUR"));

        product.rename("Gadget");
        product.reprice(Money.of("12.50", "EUR"));

        assertThat(product.name()).isEqualTo("Gadget");
        assertThat(product.listPrice()).isEqualTo(Money.of("12.50", "EUR"));
    }

    @Test
    void rejectsBlankSku() {
        assertThatThrownBy(() -> Product.create("  ", "Widget", Money.of("1.00", "EUR")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativePrice() {
        assertThatThrownBy(() -> Product.create("SKU-1", "Widget", Money.of("-1.00", "EUR")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
