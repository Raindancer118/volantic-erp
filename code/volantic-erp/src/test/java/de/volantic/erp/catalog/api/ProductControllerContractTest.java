package de.volantic.erp.catalog.api;

import de.volantic.erp.catalog.application.CatalogExceptions;
import de.volantic.erp.catalog.application.ProductService;
import de.volantic.erp.catalog.domain.model.Product;
import de.volantic.erp.catalog.domain.model.ProductId;
import de.volantic.erp.core.measure.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST v1 contract test for the product endpoints (web layer only, service mocked, security filters
 * disabled). Pins status codes, the Location header and the JSON shape so the contract can't drift.
 */
@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerContractTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        Product created = Product.create("SKU-1", "Widget", Money.of("19.99", "EUR"));
        when(productService.createProduct(eq("SKU-1"), eq("Widget"), any(Money.class))).thenReturn(created);

        mvc.perform(post("/v1/catalog/products").contentType(APPLICATION_JSON).content("""
                        {"sku":"SKU-1","name":"Widget","priceAmount":19.99,"priceCurrency":"EUR"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.endsWith("/v1/catalog/products/" + created.id().value())))
                .andExpect(jsonPath("$.id").value(created.id().value().toString()))
                .andExpect(jsonPath("$.sku").value("SKU-1"))
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.priceAmount").value(19.99))
                .andExpect(jsonPath("$.priceCurrency").value("EUR"));
    }

    @Test
    void createWithBlankSkuReturns400() throws Exception {
        mvc.perform(post("/v1/catalog/products").contentType(APPLICATION_JSON).content("""
                        {"sku":"","name":"Widget","priceAmount":1.00,"priceCurrency":"EUR"}"""))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any(), any(), any());
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        ProductId id = new ProductId(java.util.UUID.randomUUID());
        when(productService.getProduct(eq(id))).thenThrow(new CatalogExceptions.ProductNotFound(id));

        mvc.perform(get("/v1/catalog/products/{id}", id.value()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWithDuplicateSkuReturns409() throws Exception {
        when(productService.createProduct(any(), any(), any()))
                .thenThrow(new CatalogExceptions.SkuAlreadyExists("SKU-1"));

        mvc.perform(post("/v1/catalog/products").contentType(APPLICATION_JSON).content("""
                        {"sku":"SKU-1","name":"Widget","priceAmount":1.00,"priceCurrency":"EUR"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void listReturnsPagedEnvelope() throws Exception {
        when(productService.listProducts(any()))
                .thenReturn(new PageImpl<>(List.of(Product.create("SKU-1", "Widget", Money.of("1.00", "EUR")))));

        mvc.perform(get("/v1/catalog/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
