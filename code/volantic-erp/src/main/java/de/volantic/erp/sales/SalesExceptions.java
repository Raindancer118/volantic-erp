package de.volantic.erp.sales;

import de.volantic.erp.sales.domain.model.InvoiceId;

/** Application-level exceptions for the sales module (mapped to RFC-7807 problems in the api layer). */
public final class SalesExceptions {

    private SalesExceptions() {
    }

    /** No invoice exists for the given id (→ 404). */
    public static final class InvoiceNotFound extends RuntimeException {
        public InvoiceNotFound(InvoiceId id) {
            super("invoice not found: " + id.value());
        }
    }

    /** An operation was attempted that the invoice's current lifecycle state forbids (→ 409). */
    public static final class InvalidInvoiceState extends RuntimeException {
        public InvalidInvoiceState(String message) {
            super(message);
        }
    }
}
