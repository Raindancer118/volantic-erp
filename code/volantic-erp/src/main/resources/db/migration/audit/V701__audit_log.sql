-- Tamper-evident, hash-chained audit trail (GoBD/NIS2, DB architecture §4). Append-only: each row's
-- entry_hash = SHA-256(previous_hash || canonical content), so altering any past row breaks the chain.

CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE audit.audit_log (
    id            UUID                     NOT NULL PRIMARY KEY,
    version       BIGINT                   NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by    VARCHAR(255),
    modified_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by   VARCHAR(255),
    sequence      BIGINT                   NOT NULL UNIQUE,   -- monotonic chain position
    event_type    VARCHAR(150)             NOT NULL,
    entity_type   VARCHAR(150),
    entity_id     UUID,
    actor         VARCHAR(255)             NOT NULL,          -- acting OIDC subject or 'system'
    payload       TEXT,                                       -- change description (no secrets)
    occurred_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    previous_hash VARCHAR(64)              NOT NULL,          -- varchar (not char) to match the JPA String mapping
    entry_hash    VARCHAR(64)              NOT NULL UNIQUE
);

CREATE INDEX idx_audit_log_entity ON audit.audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_occurred_at ON audit.audit_log (occurred_at);
