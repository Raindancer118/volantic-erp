package de.volantic.erp.crm.api;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.crm.application.AddressService;
import de.volantic.erp.crm.domain.model.Address;
import de.volantic.erp.crm.domain.model.AddressType;
import de.volantic.erp.crm.domain.model.PartnerRef;
import de.volantic.erp.crm.domain.model.PartnerType;
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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** REST v1 contract test for address endpoints (web layer only, service mocked, filters off). */
@WebMvcTest(AddressController.class)
@AutoConfigureMockMvc(addFilters = false)
class AddressControllerContractTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AddressService addressService;

    @Test
    void createReturns201WithBody() throws Exception {
        UUID ownerId = UuidV7.randomUuid();
        Address created = Address.create(
                PartnerRef.of(PartnerType.CUSTOMER, ownerId), AddressType.BILLING, "Main St 1", "20095", "Hamburg", "DE");
        when(addressService.createAddress(any(), any(), any(), any(), any(), any())).thenReturn(created);

        mvc.perform(post("/v1/crm/addresses").contentType(APPLICATION_JSON).content("""
                        {"ownerType":"CUSTOMER","ownerId":"%s","type":"BILLING",
                         "street":"Main St 1","postalCode":"20095","city":"Hamburg","countryCode":"DE"}"""
                        .formatted(ownerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerType").value("CUSTOMER"))
                .andExpect(jsonPath("$.city").value("Hamburg"));
    }

    @Test
    void createWithBadCountryCodeReturns400() throws Exception {
        mvc.perform(post("/v1/crm/addresses").contentType(APPLICATION_JSON).content("""
                        {"ownerType":"CUSTOMER","ownerId":"%s","type":"BILLING",
                         "street":"Main St 1","postalCode":"20095","city":"Hamburg","countryCode":"DEU"}"""
                        .formatted(UuidV7.randomUuid())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listByOwnerReturns200() throws Exception {
        UUID ownerId = UuidV7.randomUuid();
        when(addressService.listAddresses(any())).thenReturn(List.of(Address.create(
                PartnerRef.of(PartnerType.CUSTOMER, ownerId), AddressType.DEFAULT, "Main St 1", "20095", "Hamburg", "DE")));

        mvc.perform(get("/v1/crm/addresses").param("ownerType", "CUSTOMER").param("ownerId", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Hamburg"));
    }
}
