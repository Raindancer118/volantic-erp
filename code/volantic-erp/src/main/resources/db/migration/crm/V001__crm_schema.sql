-- CRM / master data schema (DB architecture §4). First entity: customer.
-- Cross-module references are by UUID without FK across module boundaries (Modulith discipline).

CREATE SCHEMA IF NOT EXISTS crm;

CREATE TABLE crm.customer (
    id              UUID         NOT NULL PRIMARY KEY,
    version         BIGINT       NOT NULL,
    customer_number VARCHAR(50)  NOT NULL UNIQUE,   -- human-facing business key
    name            VARCHAR(200) NOT NULL,
    email           VARCHAR(320)
);

CREATE INDEX idx_customer_name ON crm.customer (name);
