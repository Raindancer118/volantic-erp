package de.volantic.erp.catalog.api;

import de.volantic.erp.catalog.application.BomService;
import de.volantic.erp.catalog.application.CatalogExceptions;
import de.volantic.erp.catalog.domain.model.Bom;
import de.volantic.erp.catalog.domain.model.BomLine;
import de.volantic.erp.catalog.domain.model.ProductId;
import de.volantic.erp.core.UuidV7;
import de.volantic.erp.core.measure.Quantity;
import de.volantic.erp.core.measure.UnitOfMeasure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
 * REST v1 contract test for the BOM endpoints (web layer only, service mocked, security filters
 * disabled). BOMs are nested under their owning product.
 */
@WebMvcTest(BomController.class)
@AutoConfigureMockMvc(addFilters = false)
class BomControllerContractTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BomService bomService;

    private static Bom sampleBom(UUID productId, UUID componentId) {
        BomLine line = new BomLine(new ProductId(componentId), Quantity.of("2", UnitOfMeasure.PIECE));
        return Bom.create(new ProductId(productId), 1, null, null, List.of(line));
    }

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        UUID productId = UuidV7.randomUuid();
        UUID componentId = UuidV7.randomUuid();
        Bom created = sampleBom(productId, componentId);
        when(bomService.createBom(eq(new ProductId(productId)), anyInt(), any(), any(), any())).thenReturn(created);

        mvc.perform(post("/v1/catalog/products/{productId}/boms", productId).contentType(APPLICATION_JSON).content("""
                        {"version":1,"lines":[{"componentId":"%s","quantity":2,"unit":"PCS"}]}""".formatted(componentId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.endsWith("/boms/" + created.id().value())))
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.lines[0].componentId").value(componentId.toString()))
                .andExpect(jsonPath("$.lines[0].unit").value("PCS"));
    }

    @Test
    void createWithEmptyLinesReturns400() throws Exception {
        UUID productId = UuidV7.randomUuid();

        mvc.perform(post("/v1/catalog/products/{productId}/boms", productId).contentType(APPLICATION_JSON).content("""
                        {"version":1,"lines":[]}"""))
                .andExpect(status().isBadRequest());

        verify(bomService, never()).createBom(any(), anyInt(), any(), any(), any());
    }

    @Test
    void createWithMissingProductReturns404() throws Exception {
        UUID productId = UuidV7.randomUuid();
        UUID componentId = UuidV7.randomUuid();
        when(bomService.createBom(any(), anyInt(), any(), any(), any()))
                .thenThrow(new CatalogExceptions.ProductNotFound(new ProductId(productId)));

        mvc.perform(post("/v1/catalog/products/{productId}/boms", productId).contentType(APPLICATION_JSON).content("""
                        {"version":1,"lines":[{"componentId":"%s","quantity":2,"unit":"PCS"}]}""".formatted(componentId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReturnsBomsOfProduct() throws Exception {
        UUID productId = UuidV7.randomUuid();
        UUID componentId = UuidV7.randomUuid();
        when(bomService.listBomsOfProduct(new ProductId(productId)))
                .thenReturn(List.of(sampleBom(productId, componentId)));

        mvc.perform(get("/v1/catalog/products/{productId}/boms", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value(1));
    }
}
