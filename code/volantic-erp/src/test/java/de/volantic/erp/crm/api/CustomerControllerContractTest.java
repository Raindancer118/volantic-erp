package de.volantic.erp.crm.api;

import de.volantic.erp.crm.application.CustomerNotFoundException;
import de.volantic.erp.crm.application.CustomerService;
import de.volantic.erp.crm.domain.model.Customer;
import de.volantic.erp.crm.domain.model.CustomerId;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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

    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        Customer created = Customer.create(ORG, "C-1001", "ACME GmbH", "info@acme.de");
        when(customerService.createCustomer(eq(ORG), eq("C-1001"), eq("ACME GmbH"), eq("info@acme.de")))
                .thenReturn(created);

        mvc.perform(post("/v1/crm/customers").contentType(APPLICATION_JSON).content("""
                        {"orgUnitId":"%s","customerNumber":"C-1001","name":"ACME GmbH","email":"info@acme.de"}"""
                        .formatted(ORG)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.endsWith("/v1/crm/customers/" + created.id().value())))
                .andExpect(jsonPath("$.id").value(created.id().value().toString()))
                .andExpect(jsonPath("$.orgUnitId").value(ORG.toString()))
                .andExpect(jsonPath("$.customerNumber").value("C-1001"))
                .andExpect(jsonPath("$.name").value("ACME GmbH"))
                .andExpect(jsonPath("$.email").value("info@acme.de"));
    }

    @Test
    void createWithBlankNameReturns400() throws Exception {
        mvc.perform(post("/v1/crm/customers").contentType(APPLICATION_JSON).content("""
                        {"orgUnitId":"%s","customerNumber":"C-1","name":"","email":"a@b.de"}""".formatted(ORG)))
                .andExpect(status().isBadRequest());

        verify(customerService, never()).createCustomer(any(), any(), any(), any());
    }

    @Test
    void createWithoutOrgUnitIdReturns400() throws Exception {
        mvc.perform(post("/v1/crm/customers").contentType(APPLICATION_JSON).content("""
                        {"customerNumber":"C-1","name":"ACME","email":"a@b.de"}"""))
                .andExpect(status().isBadRequest());

        verify(customerService, never()).createCustomer(any(), any(), any(), any());
    }

    @Test
    void getByIdReturns200() throws Exception {
        Customer customer = Customer.create(ORG, "C-1001", "ACME GmbH", "info@acme.de");
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
    void getByIdExposesTheVersionAsAnETag() throws Exception {
        Customer customer = Customer.reconstitute(new CustomerId(java.util.UUID.randomUUID()), 7L,
                ORG, "C-1001", "ACME GmbH", "info@acme.de");
        when(customerService.getCustomer(any(CustomerId.class))).thenReturn(customer);

        mvc.perform(get("/v1/crm/customers/{id}", customer.id().value()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"7\""))
                .andExpect(jsonPath("$.version").value(7));
    }

    @Test
    void updateWithIfMatchAppliesTheExpectedVersionAndReturnsNewETag() throws Exception {
        CustomerId id = new CustomerId(java.util.UUID.randomUUID());
        Customer updated = Customer.reconstitute(id, 8L, ORG, "C-1001", "ACME AG", "info@acme.de");
        when(customerService.updateCustomer(eq(id), eq(7L), eq("ACME AG"), eq("info@acme.de"))).thenReturn(updated);

        mvc.perform(put("/v1/crm/customers/{id}", id.value())
                        .header("If-Match", "\"7\"").contentType(APPLICATION_JSON)
                        .content("""
                        {"name":"ACME AG","email":"info@acme.de"}"""))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"8\""));

        verify(customerService).updateCustomer(eq(id), eq(7L), eq("ACME AG"), eq("info@acme.de"));
    }

    @Test
    void updateWithoutIfMatchReturns428() throws Exception {
        mvc.perform(put("/v1/crm/customers/{id}", java.util.UUID.randomUUID())
                        .contentType(APPLICATION_JSON).content("""
                        {"name":"ACME AG","email":"info@acme.de"}"""))
                .andExpect(status().isPreconditionRequired());

        verify(customerService, never()).updateCustomer(any(), any(Long.class), any(), any());
    }

    @Test
    void updateWithStaleIfMatchReturns412() throws Exception {
        when(customerService.updateCustomer(any(), eq(3L), any(), any()))
                .thenThrow(new OptimisticLockingFailureException("stale"));

        mvc.perform(put("/v1/crm/customers/{id}", java.util.UUID.randomUUID())
                        .header("If-Match", "\"3\"").contentType(APPLICATION_JSON)
                        .content("""
                        {"name":"ACME AG","email":"info@acme.de"}"""))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    void listReturnsPagedEnvelope() throws Exception {
        when(customerService.listCustomers(any()))
                .thenReturn(new PageImpl<>(List.of(Customer.create(ORG, "C-1", "ACME", "a@acme.de"))));

        mvc.perform(get("/v1/crm/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerNumber").value("C-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
