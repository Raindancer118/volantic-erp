package de.volantic.erp.sales.api;

import de.volantic.erp.sales.SalesExceptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps sales domain/application errors to RFC 7807 {@link ProblemDetail} responses. Scoped to this
 * module's api package so it does not affect other modules' error handling.
 */
@RestControllerAdvice(basePackageClasses = InvoiceController.class)
class SalesExceptionHandler {

    @ExceptionHandler(SalesExceptions.InvoiceNotFound.class)
    ProblemDetail handleNotFound(SalesExceptions.InvoiceNotFound exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /** A lifecycle violation (e.g. posting a posted invoice, cancelling a draft) → 409 Conflict. */
    @ExceptionHandler({SalesExceptions.InvalidInvoiceState.class, IllegalStateException.class})
    ProblemDetail handleConflict(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
