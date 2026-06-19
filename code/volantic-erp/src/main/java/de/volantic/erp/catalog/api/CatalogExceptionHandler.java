package de.volantic.erp.catalog.api;

import de.volantic.erp.catalog.application.CatalogExceptions;
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

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
