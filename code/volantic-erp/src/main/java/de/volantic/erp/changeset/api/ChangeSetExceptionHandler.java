package de.volantic.erp.changeset.api;

import de.volantic.erp.changeset.application.ChangeSetExceptions.ChangeSetAccessDeniedException;
import de.volantic.erp.changeset.application.ChangeSetExceptions.ChangeSetNotFoundException;
import de.volantic.erp.changeset.application.ChangeSetExceptions.FieldNotEditableException;
import de.volantic.erp.changeset.application.ChangeSetExceptions.UnknownResourceTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps change-set domain/application errors to RFC 7807 {@link ProblemDetail} responses. Scoped to this
 * module's api package so it does not affect other modules' error handling. A lifecycle violation (e.g.
 * committing an already-closed session, reverting an open Probemodus) surfaces as an
 * {@link IllegalStateException} and maps to 409 Conflict — the request is well-formed but conflicts with
 * the session's current state.
 */
@RestControllerAdvice(basePackageClasses = ChangeSetController.class)
class ChangeSetExceptionHandler {

    @ExceptionHandler(ChangeSetNotFoundException.class)
    ProblemDetail handleNotFound(ChangeSetNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ChangeSetAccessDeniedException.class)
    ProblemDetail handleAccessDenied(ChangeSetAccessDeniedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler({UnknownResourceTypeException.class, FieldNotEditableException.class})
    ProblemDetail handleUnprocessable(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleConflict(IllegalStateException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
