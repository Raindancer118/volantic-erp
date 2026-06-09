package de.volantic.erp.core.numberrange.application;

import de.volantic.erp.core.numberrange.NumberRangeNotDefinedException;
import de.volantic.erp.core.numberrange.NumberRanges;
import de.volantic.erp.core.numberrange.application.port.out.NumberRangeStore;
import de.volantic.erp.core.numberrange.domain.model.NumberRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements {@link NumberRanges}: defines ranges idempotently and allocates consecutive numbers under
 * the store's pessimistic lock. {@code next} runs in a (possibly caller-joined) transaction so the
 * counter advance and the calling document commit atomically — the gap-free GoBD guarantee.
 */
@Service
class NumberRangeService implements NumberRanges {

    private final NumberRangeStore store;

    NumberRangeService(NumberRangeStore store) {
        this.store = store;
    }

    @Override
    @Transactional
    public void defineRange(String key, String prefix, int padding, long startValue) {
        if (!store.exists(key)) {
            store.create(new NumberRange(key, prefix, padding, startValue));
        }
    }

    @Override
    @Transactional
    public String next(String key) {
        NumberRange range = store.findForUpdate(key).orElseThrow(() -> new NumberRangeNotDefinedException(key));
        String number = range.allocate();
        store.save(range);
        return number;
    }
}
