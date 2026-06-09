package de.volantic.erp.crm.api;

import de.volantic.erp.crm.application.CustomerNotFoundException;
import de.volantic.erp.crm.application.CustomerService;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
 * REST v1 contract test for the customer endpoints (web layer only, service mocked, security filters
 * disabled). Pins status codes, the Location header and the JSON shape so the contract can't drift.
 */
@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerControllerContractTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        Customer created = Customer.create("C-1001", "ACME GmbH", "info@acme.de");
        when(customerService.createCustomer("C-1001", "ACME GmbH", "info@acme.de")).thenReturn(created);

        mvc.perform(post("/v1/crm/customers").contentType(APPLICATION_JSON).content("""
                        {"customerNumber":"C-1001","name":"ACME GmbH","email":"info@acme.de"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.endsWith("/v1/crm/customers/" + created.id().value())))
                .andExpect(jsonPath("$.id").value(created.id().value().toString()))
                .andExpect(jsonPath("$.customerNumber").value("C-1001"))
                .andExpect(jsonPath("$.name").value("ACME GmbH"))
                .andExpect(jsonPath("$.email").value("info@acme.de"));
    }

    @Test
    void createWithBlankNameReturns400() throws Exception {
        mvc.perform(post("/v1/crm/customers").contentType(APPLICATION_JSON).content("""
                        {"customerNumber":"C-1","name":"","email":"a@b.de"}"""))
                .andExpect(status().isBadRequest());

        verify(customerService, never()).createCustomer(any(), any(), any());
    }

    @Test
    void getByIdReturns200() throws Exception {
        Customer customer = Customer.create("C-1001", "ACME GmbH", "info@acme.de");
        when(customerService.getCustomer(any(CustomerId.class))).thenReturn(customer);

        mvc.perform(get("/v1/crm/customers/{id}", customer.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerNumber").value("C-1001"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        CustomerId id = new CustomerId(java.util.UUID.randomUUID());
        when(customerService.getCustomer(eq(id))).thenThrow(new CustomerNotFoundException(id));

        mvc.perform(get("/v1/crm/customers/{id}", id.value()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReturns200() throws Exception {
        when(customerService.listCustomers()).thenReturn(List.of(Customer.create("C-1", "ACME", "a@acme.de")));

        mvc.perform(get("/v1/crm/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerNumber").value("C-1"));
    }
}
