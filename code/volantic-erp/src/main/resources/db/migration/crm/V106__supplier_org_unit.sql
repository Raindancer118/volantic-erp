-- ADR-0007: add org-unit scope column to crm.supplier.
-- Backfill existing rows with the seeded root org unit; then enforce NOT NULL.
ALTER TABLE crm.supplier ADD COLUMN org_unit_id UUID;
UPDATE crm.supplier SET org_unit_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE crm.supplier ALTER COLUMN org_unit_id SET NOT NULL;
CREATE INDEX idx_supplier_org_unit ON crm.supplier (org_unit_id);
