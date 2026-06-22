package de.volantic.erp.security.api;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.security.application.OrgUnitCodeAlreadyExistsException;
import de.volantic.erp.security.application.OrgUnitNotFoundException;
import de.volantic.erp.security.application.OrgUnitService;
import de.volantic.erp.security.domain.model.OrgUnit;
import de.volantic.erp.security.domain.model.OrgUnitId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
 * REST v1 contract test for the org-unit endpoints (web layer only, service mocked, security filters
 * disabled). Pins status codes, the Location/ETag headers and the JSON shape so the contract can't drift.
 */
@WebMvcTest(OrgUnitController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrgUnitControllerContractTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OrgUnitService orgUnitService;

    @Test
    void createReturns201WithLocationETagAndBody() throws Exception {
        OrgUnit created = OrgUnit.create("ROOT", "Organization", null);
        when(orgUnitService.createOrgUnit(eq("ROOT"), eq("Organization"), isNull())).thenReturn(created);

        mvc.perform(post("/v1/security/org-units").contentType(APPLICATION_JSON).content("""
                        {"code":"ROOT","name":"Organization"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.endsWith("/v1/security/org-units/" + created.id().value())))
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.id").value(created.id().value().toString()))
                .andExpect(jsonPath("$.code").value("ROOT"))
                .andExpect(jsonPath("$.name").value("Organization"))
                .andExpect(jsonPath("$.parentId").doesNotExist());
    }

    @Test
    void createWithBlankCodeReturns400() throws Exception {
        mvc.perform(post("/v1/security/org-units").contentType(APPLICATION_JSON).content("""
                        {"code":"","name":"Organization"}"""))
                .andExpect(status().isBadRequest());

        verify(orgUnitService, never()).createOrgUnit(any(), any(), any());
    }

    @Test
    void createWithDuplicateCodeReturns409() throws Exception {
        when(orgUnitService.createOrgUnit(eq("ROOT"), eq("Organization"), isNull()))
                .thenThrow(new OrgUnitCodeAlreadyExistsException("ROOT"));

        mvc.perform(post("/v1/security/org-units").contentType(APPLICATION_JSON).content("""
                        {"code":"ROOT","name":"Organization"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void getByIdReturns200WithETag() throws Exception {
        OrgUnit orgUnit = OrgUnit.reconstitute(new OrgUnitId(UuidV7.randomUuid()), 5L, null, "ROOT", "Organization");
        when(orgUnitService.getOrgUnit(any(OrgUnitId.class))).thenReturn(orgUnit);

        mvc.perform(get("/v1/security/org-units/{id}", orgUnit.id().value()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"5\""))
                .andExpect(jsonPath("$.id").value(orgUnit.id().value().toString()))
                .andExpect(jsonPath("$.code").value("ROOT"))
                .andExpect(jsonPath("$.name").value("Organization"))
                .andExpect(jsonPath("$.version").value(5));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        OrgUnitId id = new OrgUnitId(UuidV7.randomUuid());
        when(orgUnitService.getOrgUnit(any(OrgUnitId.class))).thenThrow(new OrgUnitNotFoundException(id));

        mvc.perform(get("/v1/security/org-units/{id}", id.value()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReturnsPagedEnvelope() throws Exception {
        OrgUnit orgUnit = OrgUnit.create("ROOT", "Organization", null);
        when(orgUnitService.listOrgUnits(any()))
                .thenReturn(new PageImpl<>(List.of(orgUnit)));

        mvc.perform(get("/v1/security/org-units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("ROOT"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void renameWithIfMatchReturns200WithUpdatedETag() throws Exception {
        OrgUnitId id = new OrgUnitId(UuidV7.randomUuid());
        OrgUnit renamed = OrgUnit.reconstitute(id, 4L, null, "ROOT", "Renamed Organization");
        when(orgUnitService.renameOrgUnit(any(OrgUnitId.class), eq(3L), eq("Renamed Organization")))
                .thenReturn(renamed);

        mvc.perform(put("/v1/security/org-units/{id}", id.value())
                        .header("If-Match", "\"3\"").contentType(APPLICATION_JSON)
                        .content("""
                        {"name":"Renamed Organization"}"""))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"4\""))
                .andExpect(jsonPath("$.name").value("Renamed Organization"));

        verify(orgUnitService).renameOrgUnit(any(OrgUnitId.class), eq(3L), eq("Renamed Organization"));
    }

    @Test
    void renameWithStaleIfMatchReturns412() throws Exception {
        when(orgUnitService.renameOrgUnit(any(), eq(1L), any()))
                .thenThrow(new OptimisticLockingFailureException("stale"));

        mvc.perform(put("/v1/security/org-units/{id}", UuidV7.randomUuid())
                        .header("If-Match", "\"1\"").contentType(APPLICATION_JSON)
                        .content("""
                        {"name":"New Name"}"""))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    void renameWithoutIfMatchReturns428() throws Exception {
        mvc.perform(put("/v1/security/org-units/{id}", UuidV7.randomUuid())
                        .contentType(APPLICATION_JSON).content("""
                        {"name":"New Name"}"""))
                .andExpect(status().isPreconditionRequired());

        verify(orgUnitService, never()).renameOrgUnit(any(), any(Long.class), any());
    }
}
