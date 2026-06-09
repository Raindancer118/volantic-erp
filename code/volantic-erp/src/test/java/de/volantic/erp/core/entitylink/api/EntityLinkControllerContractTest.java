package de.volantic.erp.core.entitylink.api;

import de.volantic.erp.core.entitylink.EntityLink;
import de.volantic.erp.core.entitylink.EntityLinkRegistry;
import de.volantic.erp.core.entitylink.EntityRef;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** REST v1 contract test for the 360° link endpoint (web layer only, registry mocked, filters off). */
@WebMvcTest(EntityLinkController.class)
@AutoConfigureMockMvc(addFilters = false)
class EntityLinkControllerContractTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EntityLinkRegistry registry;

    @Test
    void returnsOutgoingRelatedEntities() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        EntityRef customer = EntityRef.of("crm.customer", customerId);
        when(registry.outgoing(customer)).thenReturn(List.of(
                new EntityLink(customer, EntityRef.of("crm.contact", contactId), "HAS_CONTACT")));

        mvc.perform(get("/v1/core/entities/{type}/{id}/links", "crm.customer", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].linkType").value("HAS_CONTACT"))
                .andExpect(jsonPath("$[0].entityType").value("crm.contact"))
                .andExpect(jsonPath("$[0].entityId").value(contactId.toString()));
    }

    @Test
    void returnsIncomingWhenRequested() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        EntityRef contact = EntityRef.of("crm.contact", contactId);
        when(registry.incoming(any())).thenReturn(List.of(
                new EntityLink(EntityRef.of("crm.customer", customerId), contact, "HAS_CONTACT")));

        mvc.perform(get("/v1/core/entities/{type}/{id}/links", "crm.contact", contactId).param("direction", "incoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entityType").value("crm.customer"));
    }
}
