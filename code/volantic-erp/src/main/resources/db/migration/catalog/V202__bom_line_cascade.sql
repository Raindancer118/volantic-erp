-- bom_line is an @ElementCollection owned by catalog.bom; deleting a BOM must delete its lines. The
-- original foreign key (V201) had no ON DELETE action, so a BOM could not be removed while lines exist
-- and orphan lines were possible. Recreate the FK with ON DELETE CASCADE.

ALTER TABLE catalog.bom_line DROP CONSTRAINT bom_line_bom_id_fkey;

ALTER TABLE catalog.bom_line
    ADD CONSTRAINT bom_line_bom_id_fkey
    FOREIGN KEY (bom_id) REFERENCES catalog.bom (id) ON DELETE CASCADE;
