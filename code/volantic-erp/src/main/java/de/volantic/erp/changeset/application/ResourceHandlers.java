package de.volantic.erp.changeset.application;

import de.volantic.erp.changeset.application.ChangeSetExceptions.UnknownResourceTypeException;
import de.volantic.erp.core.revision.BulkEditHandler;
import de.volantic.erp.core.revision.ReversibleResourceHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry of the per-resource-type handlers provided by the business modules (the {@code core.revision}
 * SPI). Spring injects every {@link BulkEditHandler}/{@link ReversibleResourceHandler} bean; this indexes
 * them by resource type so the change-set service stays generic and module-agnostic (ADR-0006).
 */
@Component
public class ResourceHandlers {

    private final Map<String, BulkEditHandler> bulk;
    private final Map<String, ReversibleResourceHandler> reversible;

    public ResourceHandlers(List<BulkEditHandler> bulkHandlers, List<ReversibleResourceHandler> reversibleHandlers) {
        this.bulk = bulkHandlers.stream()
                .collect(Collectors.toUnmodifiableMap(BulkEditHandler::resourceType, Function.identity()));
        this.reversible = reversibleHandlers.stream()
                .collect(Collectors.toUnmodifiableMap(ReversibleResourceHandler::resourceType, Function.identity()));
    }

    public BulkEditHandler bulkFor(String resourceType) {
        BulkEditHandler handler = bulk.get(resourceType);
        if (handler == null) {
            throw new UnknownResourceTypeException(resourceType);
        }
        return handler;
    }

    public ReversibleResourceHandler reversibleFor(String resourceType) {
        ReversibleResourceHandler handler = reversible.get(resourceType);
        if (handler == null) {
            throw new UnknownResourceTypeException(resourceType);
        }
        return handler;
    }
}
