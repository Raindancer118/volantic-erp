package de.volantic.erp.workflow.api;

import de.volantic.erp.workflow.application.WorkflowExceptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps workflow domain/application errors to RFC 7807 {@link ProblemDetail} responses. Scoped to this
 * module's api package so it does not affect other modules' error handling.
 */
@RestControllerAdvice(basePackageClasses = ApprovalController.class)
class WorkflowExceptionHandler {

    @ExceptionHandler(WorkflowExceptions.NotFound.class)
    ProblemDetail handleNotFound(WorkflowExceptions.NotFound exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /** An unexpected workflow-engine failure → 503 (vendor exceptions never leak past this layer). */
    @ExceptionHandler(WorkflowExceptions.EngineError.class)
    ProblemDetail handleEngineError(WorkflowExceptions.EngineError exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "the workflow engine is temporarily unavailable");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
