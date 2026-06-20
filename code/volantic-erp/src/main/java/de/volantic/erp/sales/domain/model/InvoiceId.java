package de.volantic.erp.sales.domain.model;

import de.volantic.erp.core.UuidV7;

import java.util.UUID;

/** Identity of an {@link Invoice}. Time-ordered (UUIDv7) so invoices sort by creation. */
public record InvoiceId(UUID value) {

    public InvoiceId {
        if (value == null) {
            throw new IllegalArgumentException("invoice id must not be null");
        }
    }

    public static InvoiceId newId() {
        return new InvoiceId(UuidV7.randomUuid());
    }
}
