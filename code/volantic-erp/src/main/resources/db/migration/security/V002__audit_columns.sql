-- GoBD by-design traceability: standard tracking columns on every entity table (DB architecture §3).
-- Populated by Spring Data JPA auditing via AbstractEntity. timestamptz <-> OffsetDateTime.
-- The role_permission join table is a pure association (no entity) and is intentionally excluded.

ALTER TABLE security.permission
    ADD COLUMN created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN created_by  VARCHAR(255),
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN modified_by VARCHAR(255);

ALTER TABLE security.role
    ADD COLUMN created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN created_by  VARCHAR(255),
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN modified_by VARCHAR(255);

ALTER TABLE security.app_user
    ADD COLUMN created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN created_by  VARCHAR(255),
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN modified_by VARCHAR(255);

ALTER TABLE security.user_role
    ADD COLUMN created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN created_by  VARCHAR(255),
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ADD COLUMN modified_by VARCHAR(255);
