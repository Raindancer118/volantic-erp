-- ADR-0007: add org-unit scope column to sales.invoice.
-- Backfill existing rows with the seeded root org unit; then enforce NOT NULL.
ALTER TABLE sales.invoice ADD COLUMN org_unit_id UUID;
UPDATE sales.invoice SET org_unit_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE sales.invoice ALTER COLUMN org_unit_id SET NOT NULL;
CREATE INDEX idx_invoice_org_unit ON sales.invoice (org_unit_id);
