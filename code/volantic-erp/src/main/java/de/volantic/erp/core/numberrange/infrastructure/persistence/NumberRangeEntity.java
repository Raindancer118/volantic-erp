package de.volantic.erp.core.numberrange.infrastructure.persistence;

import de.volantic.erp.core.AbstractEntity;
import de.volantic.erp.core.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** JPA representation of a number range. Table {@code core.number_range}. */
@Entity
@Table(schema = "core", name = "number_range")
class NumberRangeEntity extends AbstractEntity {

    @Column(name = "range_key", nullable = false, unique = true, updatable = false)
    private String rangeKey;

    @Column(name = "prefix", nullable = false, updatable = false)
    private String prefix;

    @Column(name = "padding", nullable = false, updatable = false)
    private int padding;

    @Column(name = "next_value", nullable = false)
    private long nextValue;

    protected NumberRangeEntity() {
    }

    NumberRangeEntity(String rangeKey, String prefix, int padding, long nextValue) {
        super(UuidV7.randomUuid());
        this.rangeKey = rangeKey;
        this.prefix = prefix;
        this.padding = padding;
        this.nextValue = nextValue;
    }

    void setNextValue(long nextValue) {
        this.nextValue = nextValue;
    }

    String rangeKey() {
        return rangeKey;
    }

    String prefix() {
        return prefix;
    }

    int padding() {
        return padding;
    }

    long nextValue() {
        return nextValue;
    }
}
