-- GoBD standard tracking columns for the core entity tables (DB architecture §3).
-- entity_link already carries created_at (V901) — only the remaining audit columns are added there.

ALTER TABLE core.number_range
    ADD COLUMN created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN created_by  VARCHAR(255),
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN modified_by VARCHAR(255);

ALTER TABLE core.entity_link
    ADD COLUMN created_by  VARCHAR(255),
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN modified_by VARCHAR(255);
