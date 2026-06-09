package de.volantic.erp.workflow.api;

import de.volantic.erp.workflow.application.ApprovalService;
import de.volantic.erp.workflow.application.WorkflowExceptions;
import de.volantic.erp.workflow.domain.model.ApprovalDecision;
import de.volantic.erp.workflow.domain.model.ApprovalInstance;
import de.volantic.erp.workflow.domain.model.ApprovalStatus;
import de.volantic.erp.workflow.domain.model.ApprovalSubject;
import de.volantic.erp.workflow.domain.model.PendingApproval;
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
 * REST v1 contract test for the approval endpoints (web layer only, service mocked, security filters
 * disabled). Pins status codes, the Location header and the JSON shape so the contract can't drift.
 */
@WebMvcTest(ApprovalController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApprovalControllerContractTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ApprovalService approvalService;

    @Test
    void startReturns201WithLocationAndBody() throws Exception {
        ApprovalSubject subject = new ApprovalSubject("purchase-order", "42");
        ApprovalInstance instance = new ApprovalInstance("p-1", subject, ApprovalStatus.PENDING);
        when(approvalService.requestApproval(eq(subject), eq("alice"))).thenReturn(instance);

        mvc.perform(post("/v1/workflow/approvals").contentType(APPLICATION_JSON).content("""
                        {"subjectType":"purchase-order","subjectId":"42","requestedBy":"alice"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/v1/workflow/approvals/p-1")))
                .andExpect(jsonPath("$.instanceId").value("p-1"))
                .andExpect(jsonPath("$.subjectType").value("purchase-order"))
                .andExpect(jsonPath("$.subjectId").value("42"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void startWithBlankSubjectReturns400() throws Exception {
        mvc.perform(post("/v1/workflow/approvals").contentType(APPLICATION_JSON).content("""
                        {"subjectType":"","subjectId":"42","requestedBy":"alice"}"""))
                .andExpect(status().isBadRequest());

        verify(approvalService, never()).requestApproval(any(), any());
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(approvalService.getInstance(eq("missing")))
                .thenThrow(new WorkflowExceptions.ApprovalInstanceNotFound("missing"));

        mvc.perform(get("/v1/workflow/approvals/{id}", "missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void pendingReturnsOpenTasks() throws Exception {
        when(approvalService.pendingApprovals())
                .thenReturn(List.of(new PendingApproval("t-1", "p-1", new ApprovalSubject("purchase-order", "42"))));

        mvc.perform(get("/v1/workflow/approvals/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value("t-1"))
                .andExpect(jsonPath("$[0].subjectId").value("42"));
    }

    @Test
    void decideReturns204() throws Exception {
        mvc.perform(post("/v1/workflow/approvals/tasks/{taskId}/decision", "t-1")
                        .contentType(APPLICATION_JSON).content("""
                        {"decision":"APPROVED","decidedBy":"bob"}"""))
                .andExpect(status().isNoContent());

        verify(approvalService).decide(eq("t-1"), eq(ApprovalDecision.APPROVED), eq("bob"));
    }
}
