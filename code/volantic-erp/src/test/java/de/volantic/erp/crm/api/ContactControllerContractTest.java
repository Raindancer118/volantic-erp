package de.volantic.erp.crm.api;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.crm.application.ContactService;
import de.volantic.erp.crm.domain.model.Contact;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** REST v1 contract test for contact endpoints (web layer only, service mocked, filters off). */
@WebMvcTest(ContactController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContactControllerContractTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ContactService contactService;

    @Test
    void createReturns201WithBody() throws Exception {
        UUID ownerId = UuidV7.randomUuid();
        Contact created = Contact.create(
                PartnerRef.of(PartnerType.CUSTOMER, ownerId), "Erika", "Mustermann", "e@acme.de", null);
        when(contactService.createContact(any(), any(), any(), any(), any())).thenReturn(created);

        mvc.perform(post("/v1/crm/contacts").contentType(APPLICATION_JSON).content("""
                        {"ownerType":"CUSTOMER","ownerId":"%s","firstName":"Erika",
                         "lastName":"Mustermann","email":"e@acme.de"}""".formatted(ownerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Erika"))
                .andExpect(jsonPath("$.ownerType").value("CUSTOMER"));
    }

    @Test
    void createWithBlankLastNameReturns400() throws Exception {
        mvc.perform(post("/v1/crm/contacts").contentType(APPLICATION_JSON).content("""
                        {"ownerType":"CUSTOMER","ownerId":"%s","firstName":"Erika",
                         "lastName":"","email":"e@acme.de"}""".formatted(UuidV7.randomUuid())))
                .andExpect(status().isBadRequest());
    }
}
