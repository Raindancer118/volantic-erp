package de.volantic.erp.crm.api;

import de.volantic.erp.crm.application.CustomerNotFoundException;
import de.volantic.erp.crm.application.CustomerService;
import de.volantic.erp.crm.application.OptimisticLockException;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void createReturns201WithLocationBodyAndETag() throws Exception {
        Customer created = Customer.reconstitute(
                new CustomerId(UUID.randomUUID()), "C-1001", "ACME GmbH", "info@acme.de", 0L);
        when(customerService.createCustomer("C-1001", "ACME GmbH", "info@acme.de")).thenReturn(created);

        mvc.perform(post("/v1/crm/customers").contentType(APPLICATION_JSON).content("""
                        {"customerNumber":"C-1001","name":"ACME GmbH","email":"info@acme.de"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.endsWith("/v1/crm/customers/" + created.id().value())))
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.id").value(created.id().value().toString()))
                .andExpect(jsonPath("$.customerNumber").value("C-1001"))
                .andExpect(jsonPath("$.name").value("ACME GmbH"))
                .andExpect(jsonPath("$.email").value("info@acme.de"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void createWithBlankNameReturns400() throws Exception {
        mvc.perform(post("/v1/crm/customers").contentType(APPLICATION_JSON).content("""
                        {"customerNumber":"C-1","name":"","email":"a@b.de"}"""))
                .andExpect(status().isBadRequest());

        verify(customerService, never()).createCustomer(any(), any(), any());
    }

    @Test
    void getByIdReturns200WithETag() throws Exception {
        Customer customer = Customer.reconstitute(
                new CustomerId(UUID.randomUUID()), "C-1001", "ACME GmbH", "info@acme.de", 7L);
        when(customerService.getCustomer(any(CustomerId.class))).thenReturn(customer);

        mvc.perform(get("/v1/crm/customers/{id}", customer.id().value()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"7\""))
                .andExpect(jsonPath("$.customerNumber").value("C-1001"))
                .andExpect(jsonPath("$.version").value(7));
    }

    @Test
    void updateWithMatchingIfMatchReturns200AndNewETag() throws Exception {
        CustomerId id = new CustomerId(UUID.randomUUID());
        Customer updated = Customer.reconstitute(id, "C-1", "ACME AG", "neu@acme.de", 1L);
        when(customerService.updateCustomer(any(CustomerId.class), eq(0L), eq("ACME AG"), eq("neu@acme.de")))
                .thenReturn(updated);

        mvc.perform(put("/v1/crm/customers/{id}", id.value())
                        .header("If-Match", "\"0\"")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"ACME AG","email":"neu@acme.de"}"""))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void updateWithStaleIfMatchReturns412() throws Exception {
        when(customerService.updateCustomer(any(CustomerId.class), eq(3L), any(), any()))
                .thenThrow(new OptimisticLockException("customer x", 3L, 5L));

        mvc.perform(put("/v1/crm/customers/{id}", UUID.randomUUID())
                        .header("If-Match", "\"3\"")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"ACME AG","email":"neu@acme.de"}"""))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    void updateWithoutIfMatchReturns400() throws Exception {
        mvc.perform(put("/v1/crm/customers/{id}", UUID.randomUUID())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"ACME AG","email":"neu@acme.de"}"""))
                .andExpect(status().isBadRequest());

        verify(customerService, never()).updateCustomer(any(), anyLong(), any(), any());
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        CustomerId id = new CustomerId(java.util.UUID.randomUUID());
        when(customerService.getCustomer(eq(id))).thenThrow(new CustomerNotFoundException(id));

        mvc.perform(get("/v1/crm/customers/{id}", id.value()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReturnsPagedEnvelope() throws Exception {
        when(customerService.listCustomers(any()))
                .thenReturn(new PageImpl<>(List.of(Customer.create("C-1", "ACME", "a@acme.de"))));

        mvc.perform(get("/v1/crm/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerNumber").value("C-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
