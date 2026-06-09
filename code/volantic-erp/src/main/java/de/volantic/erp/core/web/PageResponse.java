package de.volantic.erp.core.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Stable, framework-neutral pagination envelope for REST responses. Used instead of serializing
 * Spring Data's {@code Page} directly (whose JSON shape is unstable and discouraged). Lives in the
 * core (OPEN) module so every module's api layer can reuse it.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /** Maps a Spring Data {@link Page} of domain objects onto a {@code PageResponse} of DTOs. */
    public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
