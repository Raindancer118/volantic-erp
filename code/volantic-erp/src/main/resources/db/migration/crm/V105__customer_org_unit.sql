-- ADR-0007: add org-unit scope column to crm.customer.
-- Backfill existing rows with the seeded root org unit; then enforce NOT NULL.
ALTER TABLE crm.customer ADD COLUMN org_unit_id UUID;
UPDATE crm.customer SET org_unit_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE crm.customer ALTER COLUMN org_unit_id SET NOT NULL;
CREATE INDEX idx_customer_org_unit ON crm.customer (org_unit_id);
