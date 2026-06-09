package de.volantic.erp.crm.api;

import de.volantic.erp.crm.application.SupplierService;
import de.volantic.erp.crm.domain.model.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** REST v1 contract test for supplier endpoints (web layer only, service mocked, filters off). */
@WebMvcTest(SupplierController.class)
@AutoConfigureMockMvc(addFilters = false)
class SupplierControllerContractTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SupplierService supplierService;

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        Supplier created = Supplier.create("S-1001", "Globex", "sales@globex.de");
        when(supplierService.createSupplier("S-1001", "Globex", "sales@globex.de")).thenReturn(created);

        mvc.perform(post("/v1/crm/suppliers").contentType(APPLICATION_JSON).content("""
                        {"supplierNumber":"S-1001","name":"Globex","email":"sales@globex.de"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.endsWith("/v1/crm/suppliers/" + created.id().value())))
                .andExpect(jsonPath("$.supplierNumber").value("S-1001"))
                .andExpect(jsonPath("$.name").value("Globex"));
    }

    @Test
    void createWithBlankNameReturns400() throws Exception {
        mvc.perform(post("/v1/crm/suppliers").contentType(APPLICATION_JSON).content("""
                        {"supplierNumber":"S-1","name":"","email":"a@b.de"}"""))
                .andExpect(status().isBadRequest());
    }
}
