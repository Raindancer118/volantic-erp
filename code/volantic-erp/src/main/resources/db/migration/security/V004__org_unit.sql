-- Organizational units: the hierarchical scope dimension for instance-level authorization (ADR-0007).
-- A unit with parent_id = NULL is a root; the parent FK stays within this module's schema (intra-module,
-- allowed). The tree is small and changes rarely; the application caches it for the authorization hot path.

CREATE TABLE security.org_unit (
    id          UUID         NOT NULL PRIMARY KEY,
    version     BIGINT       NOT NULL,
    parent_id   UUID         REFERENCES security.org_unit (id),   -- NULL = root
    code        VARCHAR(50)  NOT NULL UNIQUE,                      -- human-facing business key
    name        VARCHAR(200) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by  VARCHAR(255),
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    modified_by VARCHAR(255)
);

CREATE INDEX idx_org_unit_parent ON security.org_unit (parent_id);

-- Seed the default root unit. Master data created before org scoping is backfilled to it by the
-- per-module migrations (crm V105, sales V402) so nothing becomes inaccessible. The id is fixed so those
-- migrations can reference it without a lookup.
INSERT INTO security.org_unit (id, version, parent_id, code, name)
VALUES ('00000000-0000-0000-0000-000000000001', 0, NULL, 'ROOT', 'Organization');
