package de.volantic.erp.catalog.api;

import de.volantic.erp.catalog.application.CatalogExceptions;
import de.volantic.erp.core.web.ETags;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps catalog domain/application errors to RFC 7807 {@link ProblemDetail} responses. Scoped to this
 * module's api package so it does not affect other modules' error handling. Works for every catalog
 * entity via the sealed {@link CatalogExceptions.NotFound}/{@link CatalogExceptions.Conflict} bases.
 */
@RestControllerAdvice(basePackageClasses = ProductController.class)
class CatalogExceptionHandler {

    @ExceptionHandler(CatalogExceptions.NotFound.class)
    ProblemDetail handleNotFound(CatalogExceptions.NotFound exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(CatalogExceptions.Conflict.class)
    ProblemDetail handleConflict(CatalogExceptions.Conflict exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(CatalogExceptions.CircularBom.class)
    ProblemDetail handleCircularBom(CatalogExceptions.CircularBom exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    /** A stale If-Match version (the resource changed since the client read it) → 412 Precondition Failed. */
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
