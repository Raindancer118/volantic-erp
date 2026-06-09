-- CRM supplier master data (DB architecture §4).

CREATE TABLE crm.supplier (
    id              UUID         NOT NULL PRIMARY KEY,
    version         BIGINT       NOT NULL,
    supplier_number VARCHAR(50)  NOT NULL UNIQUE,   -- human-facing business key
    name            VARCHAR(200) NOT NULL,
    email           VARCHAR(320)
);

CREATE INDEX idx_supplier_name ON crm.supplier (name);
