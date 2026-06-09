package de.volantic.erp.core.numberrange.application.port.out;

import de.volantic.erp.core.numberrange.domain.model.NumberRange;

import java.util.Optional;

/**
 * Outbound port for number-range persistence. {@link #findForUpdate(String)} must acquire a pessimistic
 * write lock on the row so concurrent allocations are serialized — the basis for the gap-free guarantee.
 */
public interface NumberRangeStore {

    boolean exists(String key);

    void create(NumberRange range);

    /** Loads the range under a pessimistic write lock (must be called within a transaction). */
    Optional<NumberRange> findForUpdate(String key);

    void save(NumberRange range);
}
