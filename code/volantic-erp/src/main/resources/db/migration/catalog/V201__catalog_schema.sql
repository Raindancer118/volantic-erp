-- Catalog schema (DB architecture §5): products and their versioned bills of materials.
-- Cross-module references are by UUID without FK across module boundaries (Modulith discipline);
-- BOM component ids reference catalog.product within the same module, kept by the application.

CREATE SCHEMA IF NOT EXISTS catalog;

CREATE TABLE catalog.product (
    id             UUID         NOT NULL PRIMARY KEY,
    version        BIGINT       NOT NULL,
    sku            VARCHAR(50)  NOT NULL UNIQUE,    -- human-facing business key (stock keeping unit)
    name           VARCHAR(200) NOT NULL,
    price_amount   NUMERIC(19, 4) NOT NULL,
    price_currency VARCHAR(3)   NOT NULL,           -- ISO 4217; varchar (not char) to match the JPA String mapping under ddl-auto=validate
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by     VARCHAR(255),
    modified_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    modified_by    VARCHAR(255)
);

CREATE INDEX idx_product_name ON catalog.product (name);

CREATE TABLE catalog.bom (
    id          UUID    NOT NULL PRIMARY KEY,
    version     BIGINT  NOT NULL,
    product_id  UUID    NOT NULL,                   -- owning product (catalog.product.id)
    bom_version INTEGER NOT NULL,                   -- business version of the BOM (immutable once created)
    valid_from  DATE,
    valid_to    DATE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by  VARCHAR(255),
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    modified_by VARCHAR(255),
    CONSTRAINT uq_bom_product_version UNIQUE (product_id, bom_version)
);

CREATE INDEX idx_bom_product ON catalog.bom (product_id);

-- BOM lines as an element collection owned by catalog.bom (no own identity / audit columns).
CREATE TABLE catalog.bom_line (
    bom_id       UUID           NOT NULL REFERENCES catalog.bom (id),
    component_id UUID           NOT NULL,           -- referenced product (catalog.product.id)
    qty_amount   NUMERIC(19, 4) NOT NULL,
    qty_unit     VARCHAR(20)    NOT NULL
);

CREATE INDEX idx_bom_line_bom ON catalog.bom_line (bom_id);
