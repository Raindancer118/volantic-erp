package de.volantic.erp.workflow.application;

/** Error hierarchy for the workflow module, mapped to HTTP status by the api layer. */
public final class WorkflowExceptions {

    private WorkflowExceptions() {
    }

    /** Mapped to HTTP 404. */
    public abstract static sealed class NotFound extends RuntimeException
            permits ApprovalInstanceNotFound, ApprovalTaskNotFound {
        protected NotFound(String message) {
            super(message);
        }
    }

    public static final class ApprovalInstanceNotFound extends NotFound {
        public ApprovalInstanceNotFound(String instanceId) {
            super("approval instance not found: " + instanceId);
        }
    }

    public static final class ApprovalTaskNotFound extends NotFound {
        public ApprovalTaskNotFound(String taskId) {
            super("approval task not found or already completed: " + taskId);
        }
    }

    /**
     * The underlying workflow engine failed unexpectedly. Mapped to HTTP 503 — keeps vendor (Flowable)
     * exceptions from leaking into the domain and REST layers.
     */
    public static final class EngineError extends RuntimeException {
        public EngineError(String operation, Throwable cause) {
            super("workflow engine failed during " + operation, cause);
        }
    }
}
