-- Sales schema (DB architecture §5.1). Invoices are the canonical "Beleg": a draft until posted, then a
-- gap-free numbered, immutable legal document; corrected only by a storno document (ADR-0006 §2).
-- V4xx hundreds-block for the sales module (security V0xx, crm V1xx, catalog V2xx, changeset V3xx,
-- sales V4xx, audit V7xx, core V9xx) — global uniqueness guarded by FlywayMigrationVersionsTest.

CREATE SCHEMA IF NOT EXISTS sales;

CREATE TABLE sales.invoice (
    id              UUID         NOT NULL PRIMARY KEY,
    version         BIGINT       NOT NULL,
    customer_id     UUID         NOT NULL,                 -- cross-module ref to crm.customer (no FK)
    currency        VARCHAR(3)   NOT NULL,
    status          VARCHAR(20)  NOT NULL,                 -- DRAFT / POSTED / CANCELLED
    document_number VARCHAR(50)  UNIQUE,                   -- gap-free, assigned on post; null while draft
    issue_date      DATE,
    storno_of       UUID,                                  -- the invoice this storno cancels (self-ref by id)
    cancelled_by    UUID,                                  -- the storno that cancelled this invoice
    -- GoBD audit/tracking columns (see core.AbstractEntity)
    created_at      TIMESTAMPTZ  NOT NULL,
    created_by      VARCHAR(255),
    modified_at     TIMESTAMPTZ  NOT NULL,
    modified_by     VARCHAR(255)
);

-- Invoice lines as an element collection owned by sales.invoice (no own identity / audit columns).
CREATE TABLE sales.invoice_line (
    invoice_id        UUID           NOT NULL REFERENCES sales.invoice (id) ON DELETE CASCADE,
    description       VARCHAR(500)   NOT NULL,
    quantity          NUMERIC(19, 4) NOT NULL,
    unit_price_amount NUMERIC(19, 4) NOT NULL
);

CREATE INDEX idx_invoice_line_invoice ON sales.invoice_line (invoice_id);
CREATE INDEX idx_invoice_customer ON sales.invoice (customer_id);
