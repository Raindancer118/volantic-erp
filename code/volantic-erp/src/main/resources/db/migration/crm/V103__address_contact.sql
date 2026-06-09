-- CRM addresses and contacts, owned by a partner (customer or supplier) via a flat (owner_type,
-- owner_id) reference — no cross-table FK, integrity kept by the application (DB architecture §4).

CREATE TABLE crm.address (
    id           UUID         NOT NULL PRIMARY KEY,
    version      BIGINT       NOT NULL,
    owner_type   VARCHAR(20)  NOT NULL,            -- CUSTOMER | SUPPLIER
    owner_id     UUID         NOT NULL,
    type         VARCHAR(20)  NOT NULL,            -- BILLING | SHIPPING | DEFAULT
    street       VARCHAR(200) NOT NULL,
    postal_code  VARCHAR(20)  NOT NULL,
    city         VARCHAR(100) NOT NULL,
    country_code VARCHAR(2)   NOT NULL   -- ISO 3166-1 alpha-2; varchar (not char) to match the JPA String mapping under ddl-auto=validate
);

CREATE INDEX idx_address_owner ON crm.address (owner_type, owner_id);

CREATE TABLE crm.contact (
    id         UUID         NOT NULL PRIMARY KEY,
    version    BIGINT       NOT NULL,
    owner_type VARCHAR(20)  NOT NULL,              -- CUSTOMER | SUPPLIER
    owner_id   UUID         NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(320),
    phone      VARCHAR(50)
);

CREATE INDEX idx_contact_owner ON crm.contact (owner_type, owner_id);
