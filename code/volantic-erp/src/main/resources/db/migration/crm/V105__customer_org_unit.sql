-- Org-unit data scope for customers (ADR-0004 fine-grained authorization). Each customer belongs to an
-- organizational unit; authorization can be granted globally or scoped to specific org units
-- (AccessScope type ORG_UNIT). Existing rows are backfilled to the well-known default/HQ unit, so the
-- change is backward-compatible — globally granted users keep full access, only scoped grants restrict.

ALTER TABLE crm.customer ADD COLUMN org_unit_id UUID;

UPDATE crm.customer
SET org_unit_id = '00000000-0000-0000-0000-000000000001'
WHERE org_unit_id IS NULL;

ALTER TABLE crm.customer ALTER COLUMN org_unit_id SET NOT NULL;
ALTER TABLE crm.customer ALTER COLUMN org_unit_id SET DEFAULT '00000000-0000-0000-0000-000000000001';

CREATE INDEX idx_customer_org_unit ON crm.customer (org_unit_id);
