package de.volantic.erp.core.numberrange.infrastructure.persistence;

import de.volantic.erp.core.numberrange.application.port.out.NumberRangeStore;
import de.volantic.erp.core.numberrange.domain.model.NumberRange;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Outbound adapter for {@link NumberRangeStore}. Maps between the domain {@link NumberRange} and the JPA
 * entity, and on save copies the advanced counter back onto the locked row.
 */
@Component
class NumberRangeStoreAdapter implements NumberRangeStore {

    private final NumberRangeJpaRepository jpa;

    NumberRangeStoreAdapter(NumberRangeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean exists(String key) {
        return jpa.existsByRangeKey(key);
    }

    @Override
    public void create(NumberRange range) {
        jpa.save(new NumberRangeEntity(range.key(), range.prefix(), range.padding(), range.nextValue()));
    }

    @Override
    public Optional<NumberRange> findForUpdate(String key) {
        return jpa.findForUpdateByRangeKey(key).map(NumberRangeStoreAdapter::toDomain);
    }

    @Override
    public void save(NumberRange range) {
        // The locked row is re-loaded (managed in the same transaction) and its counter updated.
        jpa.findByRangeKey(range.key()).ifPresent(entity -> entity.setNextValue(range.nextValue()));
    }

    private static NumberRange toDomain(NumberRangeEntity entity) {
        return new NumberRange(entity.rangeKey(), entity.prefix(), entity.padding(), entity.nextValue());
    }
}
