package de.volantic.erp.security.api;

import de.volantic.erp.core.web.ETags;
import de.volantic.erp.security.application.OrgUnitCodeAlreadyExistsException;
import de.volantic.erp.security.application.OrgUnitNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps security-module api errors (org-unit management) to RFC 7807 {@link ProblemDetail} responses.
 * Scoped to this module's api package. Access-denied (403) is handled by Spring Security globally.
 */
@RestControllerAdvice(basePackageClasses = OrgUnitController.class)
class SecurityExceptionHandler {

    @ExceptionHandler(OrgUnitNotFoundException.class)
    ProblemDetail handleNotFound(OrgUnitNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(OrgUnitCodeAlreadyExistsException.class)
    ProblemDetail handleConflict(OrgUnitCodeAlreadyExistsException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    /** A stale If-Match version (the unit changed since the client read it) → 412 Precondition Failed. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleStale(OptimisticLockingFailureException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_FAILED,
                "the resource was modified concurrently; re-read it and retry");
    }

    /** A conditional update without the required If-Match header → 428 Precondition Required. */
    @ExceptionHandler(ETags.IfMatchRequiredException.class)
    ProblemDetail handleMissingIfMatch(ETags.IfMatchRequiredException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_REQUIRED, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
