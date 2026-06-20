package de.volantic.erp.changeset.api;

import de.volantic.erp.changeset.application.BulkChange;
import de.volantic.erp.changeset.application.BulkPreview;
import de.volantic.erp.changeset.application.ChangeSetExceptions.ChangeSetAccessDeniedException;
import de.volantic.erp.changeset.application.ChangeSetExceptions.ChangeSetNotFoundException;
import de.volantic.erp.changeset.application.ChangeSetExceptions.FieldNotEditableException;
import de.volantic.erp.changeset.application.ChangeSetExceptions.UnknownResourceTypeException;
import de.volantic.erp.changeset.application.ChangeSetService;
import de.volantic.erp.changeset.domain.model.ChangeSetId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST v1 contract test for the change-set endpoints (web layer only, service mocked, security filters
 * disabled). Pins status codes, the Location header and the JSON/ProblemDetail shapes so the API of the
 * Rollback Engine and Probemodus (ADR-0006) cannot drift.
 */
@WebMvcTest(ChangeSetController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChangeSetControllerContractTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChangeSetService changeSets;

    @Test
    void beginLiveReturns201WithLocationAndId() throws Exception {
        ChangeSetId id = ChangeSetId.newId();
        when(changeSets.beginLive()).thenReturn(id);

        mvc.perform(post("/v1/changeset/sessions").contentType(APPLICATION_JSON).content("""
                        {"mode":"LIVE"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.endsWith("/v1/changeset/sessions/" + id.value())))
                .andExpect(jsonPath("$.id").value(id.value().toString()))
                .andExpect(jsonPath("$.mode").value("LIVE"));

        verify(changeSets).beginLive();
        verify(changeSets, never()).beginProbemodus();
    }

    @Test
    void beginProbemodusRoutesToProbemodusUseCase() throws Exception {
        ChangeSetId id = ChangeSetId.newId();
        when(changeSets.beginProbemodus()).thenReturn(id);

        mvc.perform(post("/v1/changeset/sessions").contentType(APPLICATION_JSON).content("""
                        {"mode":"DEFERRED"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mode").value("DEFERRED"));

        verify(changeSets).beginProbemodus();
        verify(changeSets, never()).beginLive();
    }

    @Test
    void beginWithMissingModeReturns400() throws Exception {
        mvc.perform(post("/v1/changeset/sessions").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verify(changeSets, never()).beginLive();
        verify(changeSets, never()).beginProbemodus();
    }

    @Test
    void beginWithUnknownModeReturns400() throws Exception {
        mvc.perform(post("/v1/changeset/sessions").contentType(APPLICATION_JSON).content("""
                        {"mode":"WHATEVER"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void applyReturns204AndForwardsTheBulkChange() throws Exception {
        ChangeSetId id = ChangeSetId.newId();
        UUID target = UUID.randomUUID();

        mvc.perform(post("/v1/changeset/sessions/{id}/apply", id.value())
                        .contentType(APPLICATION_JSON).content("""
                        {"resourceType":"crm.customer","ids":["%s"],"fieldChanges":{"city":"Hamburg"}}"""
                        .formatted(target)))
                .andExpect(status().isNoContent());

        ArgumentCaptor<ChangeSetId> sessionCaptor = ArgumentCaptor.forClass(ChangeSetId.class);
        ArgumentCaptor<BulkChange> changeCaptor = ArgumentCaptor.forClass(BulkChange.class);
        verify(changeSets).apply(sessionCaptor.capture(), changeCaptor.capture());
        assertThat(sessionCaptor.getValue().value()).isEqualTo(id.value());
        assertThat(changeCaptor.getValue().resourceType()).isEqualTo("crm.customer");
        assertThat(changeCaptor.getValue().ids()).containsExactly(target);
        assertThat(changeCaptor.getValue().fieldChanges()).containsEntry("city", "Hamburg");
    }

    @Test
    void applyWithBlankResourceTypeReturns400() throws Exception {
        mvc.perform(post("/v1/changeset/sessions/{id}/apply", UUID.randomUUID())
                        .contentType(APPLICATION_JSON).content("""
                        {"resourceType":"","ids":[],"fieldChanges":{}}"""))
                .andExpect(status().isBadRequest());

        verify(changeSets, never()).apply(any(), any());
    }

    @Test
    void previewReturns200WithRowsAndProblemFlag() throws Exception {
        UUID present = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        when(changeSets.preview(any(BulkChange.class))).thenReturn(new BulkPreview("crm.customer", List.of(
                new BulkPreview.Row(present, true, "{\"city\":\"Berlin\"}", null),
                new BulkPreview.Row(missing, false, null, "resource does not exist"))));

        mvc.perform(post("/v1/changeset/preview").contentType(APPLICATION_JSON).content("""
                        {"resourceType":"crm.customer","ids":["%s","%s"],"fieldChanges":{"city":"Hamburg"}}"""
                        .formatted(present, missing)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("crm.customer"))
                .andExpect(jsonPath("$.hasProblems").value(true))
                .andExpect(jsonPath("$.rows[0].id").value(present.toString()))
                .andExpect(jsonPath("$.rows[0].applicable").value(true))
                .andExpect(jsonPath("$.rows[1].applicable").value(false))
                .andExpect(jsonPath("$.rows[1].problem").value("resource does not exist"));
    }

    @Test
    void commitReturns204() throws Exception {
        mvc.perform(post("/v1/changeset/sessions/{id}/commit", UUID.randomUUID()))
                .andExpect(status().isNoContent());

        verify(changeSets).commit(any(ChangeSetId.class));
    }

    @Test
    void discardReturns204() throws Exception {
        mvc.perform(post("/v1/changeset/sessions/{id}/discard", UUID.randomUUID()))
                .andExpect(status().isNoContent());

        verify(changeSets).discard(any(ChangeSetId.class));
    }

    @Test
    void revertReturns204() throws Exception {
        mvc.perform(post("/v1/changeset/sessions/{id}/revert", UUID.randomUUID()))
                .andExpect(status().isNoContent());

        verify(changeSets).revert(any(ChangeSetId.class));
    }

    @Test
    void unknownSessionReturns404() throws Exception {
        ChangeSetId id = ChangeSetId.newId();
        doThrow(new ChangeSetNotFoundException(id)).when(changeSets).commit(any());

        mvc.perform(post("/v1/changeset/sessions/{id}/commit", id.value()))
                .andExpect(status().isNotFound());
    }

    @Test
    void operatingOnAnotherActorsSessionReturns403() throws Exception {
        ChangeSetId id = ChangeSetId.newId();
        doThrow(new ChangeSetAccessDeniedException(id)).when(changeSets).revert(any());

        mvc.perform(post("/v1/changeset/sessions/{id}/revert", id.value()))
                .andExpect(status().isForbidden());
    }

    @Test
    void revertingInAnInvalidStateReturns409() throws Exception {
        doThrow(new IllegalStateException("an open Probemodus session is discarded, not reverted"))
                .when(changeSets).revert(any());

        mvc.perform(post("/v1/changeset/sessions/{id}/revert", UUID.randomUUID()))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownResourceTypeReturns422() throws Exception {
        when(changeSets.preview(any())).thenThrow(new UnknownResourceTypeException("ghost.entity"));

        mvc.perform(post("/v1/changeset/preview").contentType(APPLICATION_JSON).content("""
                        {"resourceType":"ghost.entity","ids":[],"fieldChanges":{}}"""))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void uneditableFieldReturns422() throws Exception {
        doThrow(new FieldNotEditableException("crm.customer", "number")).when(changeSets).apply(any(), any());

        mvc.perform(post("/v1/changeset/sessions/{id}/apply", UUID.randomUUID())
                        .contentType(APPLICATION_JSON).content("""
                        {"resourceType":"crm.customer","ids":["%s"],"fieldChanges":{"number":"X"}}"""
                        .formatted(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity());
    }
}
