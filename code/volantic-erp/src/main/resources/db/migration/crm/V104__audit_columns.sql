-- GoBD standard tracking columns for the crm entity tables (DB architecture §3).

ALTER TABLE crm.customer
    ADD COLUMN created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN created_by  VARCHAR(255),
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN modified_by VARCHAR(255);

ALTER TABLE crm.supplier
    ADD COLUMN created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN created_by  VARCHAR(255),
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN modified_by VARCHAR(255);

ALTER TABLE crm.address
    ADD COLUMN created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN created_by  VARCHAR(255),
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN modified_by VARCHAR(255);

ALTER TABLE crm.contact
    ADD COLUMN created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN created_by  VARCHAR(255),
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN modified_by VARCHAR(255);
