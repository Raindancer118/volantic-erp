package de.volantic.erp.crm.api;

import de.volantic.erp.crm.application.CrmConflictException;
import de.volantic.erp.crm.application.CrmNotFoundException;
import de.volantic.erp.crm.application.OptimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps CRM domain/application errors to RFC 7807 {@link ProblemDetail} responses. Scoped to this
 * module's api package so it does not affect other modules' error handling. Works for every CRM
 * entity via the shared {@link CrmNotFoundException}/{@link CrmConflictException} base types.
 */
@RestControllerAdvice(basePackageClasses = CustomerController.class)
class CrmExceptionHandler {

    @ExceptionHandler(CrmNotFoundException.class)
    ProblemDetail handleNotFound(CrmNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(CrmConflictException.class)
    ProblemDetail handleConflict(CrmConflictException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(OptimisticLockException.class)
    ProblemDetail handleStaleUpdate(OptimisticLockException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_FAILED, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
