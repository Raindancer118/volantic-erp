package de.volantic.erp.sales.api;

import de.volantic.erp.core.measure.Money;
import de.volantic.erp.sales.SalesExceptions;
import de.volantic.erp.sales.application.InvoiceService;
import de.volantic.erp.sales.domain.model.Invoice;
import de.volantic.erp.sales.domain.model.InvoiceId;
import de.volantic.erp.sales.domain.model.InvoiceLine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST v1 contract test for the invoice endpoints (web layer only, service mocked, security filters
 * disabled). Pins status codes and the JSON/ProblemDetail shapes so the Beleg contract cannot drift.
 */
@WebMvcTest(InvoiceController.class)
@AutoConfigureMockMvc(addFilters = false)
class InvoiceControllerContractTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private InvoiceService invoiceService;

    private static Invoice draft() {
        return Invoice.createDraft(UUID.randomUUID(), Currency.getInstance("EUR"),
                List.of(new InvoiceLine("Widget", new BigDecimal("2"), Money.of("10.00", "EUR"))));
    }

    @Test
    void createReturns201WithBody() throws Exception {
        when(invoiceService.createDraft(any(), any(), any())).thenReturn(draft());

        mvc.perform(post("/v1/sales/invoices").contentType(APPLICATION_JSON).content("""
                        {"customerId":"%s","currency":"EUR",
                         "lines":[{"description":"Widget","quantity":2,"unitPrice":10.00}]}"""
                        .formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.total").value(20.00))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(20.00));
    }

    @Test
    void createWithNoLinesReturns400() throws Exception {
        mvc.perform(post("/v1/sales/invoices").contentType(APPLICATION_JSON).content("""
                        {"customerId":"%s","currency":"EUR","lines":[]}""".formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        verify(invoiceService, never()).createDraft(any(), any(), any());
    }

    @Test
    void postReturnsPostedInvoiceWithNumber() throws Exception {
        Invoice posted = draft();
        posted.post("RE-000001");
        when(invoiceService.post(any(InvoiceId.class))).thenReturn(posted);

        mvc.perform(post("/v1/sales/invoices/{id}/post", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.documentNumber").value("RE-000001"));
    }

    @Test
    void cancelReturns201WithTheStornoDocument() throws Exception {
        Invoice original = draft();
        original.post("RE-000001");
        Invoice storno = Invoice.storno(original);
        storno.post("RE-000002");
        when(invoiceService.cancel(any(InvoiceId.class))).thenReturn(storno);

        mvc.perform(post("/v1/sales/invoices/{id}/cancel", original.id().value()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentNumber").value("RE-000002"))
                .andExpect(jsonPath("$.stornoOf").value(original.id().value().toString()))
                .andExpect(jsonPath("$.total").value(-20.00));
    }

    @Test
    void postingAlreadyPostedReturns409() throws Exception {
        when(invoiceService.post(any())).thenThrow(new SalesExceptions.InvalidInvoiceState("already posted"));

        mvc.perform(post("/v1/sales/invoices/{id}/post", UUID.randomUUID()))
                .andExpect(status().isConflict());
    }

    @Test
    void getMissingReturns404() throws Exception {
        InvoiceId id = InvoiceId.newId();
        when(invoiceService.getInvoice(any())).thenThrow(new SalesExceptions.InvoiceNotFound(id));

        mvc.perform(get("/v1/sales/invoices/{id}", id.value()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReturnsPagedEnvelope() throws Exception {
        when(invoiceService.listInvoices(any())).thenReturn(new PageImpl<>(List.of(draft())));

        mvc.perform(get("/v1/sales/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
