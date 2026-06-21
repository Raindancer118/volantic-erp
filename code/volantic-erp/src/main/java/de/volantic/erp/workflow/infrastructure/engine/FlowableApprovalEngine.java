package de.volantic.erp.workflow.infrastructure.engine;

import de.volantic.erp.workflow.application.WorkflowExceptions;
import de.volantic.erp.workflow.application.port.out.ApprovalEngine;
import de.volantic.erp.workflow.domain.model.ApprovalDecision;
import de.volantic.erp.workflow.domain.model.ApprovalInstance;
import de.volantic.erp.workflow.domain.model.ApprovalStatus;
import de.volantic.erp.workflow.domain.model.ApprovalSubject;
import de.volantic.erp.workflow.domain.model.PendingApproval;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Flowable-backed {@link ApprovalEngine}. Maps the generic {@code approval} BPMN process
 * (see {@code processes/approval.bpmn20.xml}) onto the domain port: start, list open tasks, record a
 * decision, and resolve the status of a running or finished instance. The process is the only Flowable
 * detail the rest of the ERP ever touches — through this adapter.
 */
@Component
class FlowableApprovalEngine implements ApprovalEngine {

    static final String PROCESS_KEY = "approval";
    /** Upper bound on the open-task list so an unbounded inbox cannot exhaust memory (audit finding). */
    static final int MAX_PENDING_TASKS = 500;
    static final String VAR_SUBJECT_TYPE = "subjectType";
    static final String VAR_SUBJECT_ID = "subjectId";
    static final String VAR_REQUESTED_BY = "requestedBy";
    static final String VAR_APPROVED = "approved";
    static final String VAR_DECIDED_BY = "decidedBy";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    FlowableApprovalEngine(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
    }

    @Override
    public ApprovalInstance start(ApprovalSubject subject, String requestedBy) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(VAR_SUBJECT_TYPE, subject.type());
        variables.put(VAR_SUBJECT_ID, subject.id());
        variables.put(VAR_REQUESTED_BY, requestedBy);
        try {
            ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, variables);
            return new ApprovalInstance(instance.getId(), subject, ApprovalStatus.PENDING);
        } catch (FlowableException e) {
            throw new WorkflowExceptions.EngineError("start", e);
        }
    }

    @Override
    public List<PendingApproval> pendingApprovals() {
        try {
            // Bounded fetch (listPage) — never an unbounded SELECT that could exhaust memory.
            List<Task> tasks = taskService.createTaskQuery()
                    .processDefinitionKey(PROCESS_KEY)
                    .includeProcessVariables()
                    .active()
                    .orderByTaskCreateTime().asc()
                    .listPage(0, MAX_PENDING_TASKS);
            return tasks.stream()
                    .map(task -> new PendingApproval(
                            task.getId(), task.getProcessInstanceId(), subjectOf(task.getProcessVariables())))
                    .toList();
        } catch (FlowableException e) {
            throw new WorkflowExceptions.EngineError("pendingApprovals", e);
        }
    }

    @Override
    public void decide(String taskId, ApprovalDecision decision, String decidedBy) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(VAR_APPROVED, decision == ApprovalDecision.APPROVED);
        variables.put(VAR_DECIDED_BY, decidedBy);
        try {
            taskService.complete(taskId, variables);
        } catch (FlowableObjectNotFoundException notFound) {
            throw new WorkflowExceptions.ApprovalTaskNotFound(taskId);
        } catch (FlowableException e) {
            throw new WorkflowExceptions.EngineError("decide", e);
        }
    }

    @Override
    public Optional<ApprovalInstance> findInstance(String instanceId) {
        try {
            ProcessInstance running = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(instanceId)
                    .singleResult();
            if (running != null) {
                return Optional.of(new ApprovalInstance(
                        instanceId, subjectOf(runtimeService.getVariables(instanceId)), ApprovalStatus.PENDING));
            }

            HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(instanceId)
                    .singleResult();
            if (historic == null) {
                return Optional.empty();
            }
            Map<String, Object> variables = historicVariables(instanceId);
            ApprovalStatus status = Boolean.TRUE.equals(variables.get(VAR_APPROVED))
                    ? ApprovalStatus.APPROVED
                    : ApprovalStatus.REJECTED;
            return Optional.of(new ApprovalInstance(instanceId, subjectOf(variables), status));
        } catch (FlowableException e) {
            throw new WorkflowExceptions.EngineError("findInstance", e);
        }
    }

    private Map<String, Object> historicVariables(String instanceId) {
        List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(instanceId)
                .list();
        Map<String, Object> result = new HashMap<>();
        for (HistoricVariableInstance variable : variables) {
            result.put(variable.getVariableName(), variable.getValue());
        }
        return result;
    }

    private static ApprovalSubject subjectOf(Map<String, Object> variables) {
        return new ApprovalSubject(
                String.valueOf(variables.get(VAR_SUBJECT_TYPE)),
                String.valueOf(variables.get(VAR_SUBJECT_ID)));
    }
}
