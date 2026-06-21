-- Reversible edit sessions (ADR-0006): mass edits, Probemodus (deferred apply) and the Rollback Engine.
-- One row per session; the recorded operations are stored as a JSON array in `operations` (opaque
-- before-state snapshots / change payloads). Forward-only by design — reverting writes compensating
-- operations, it never deletes rows here or in the audit trail.

CREATE SCHEMA IF NOT EXISTS changeset;

CREATE TABLE changeset.change_set (
    id          UUID                     NOT NULL PRIMARY KEY,
    version     BIGINT                   NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(255),
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR(255),
    actor       VARCHAR(255)             NOT NULL,          -- who opened the session (OIDC subject or 'system')
    mode        VARCHAR(20)              NOT NULL,          -- LIVE | DEFERRED (Probemodus)
    status      VARCHAR(20)              NOT NULL,          -- OPEN | COMMITTED | REVERTED | DISCARDED
    opened_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at   TIMESTAMP WITH TIME ZONE,
    operations  TEXT                     NOT NULL DEFAULT '[]'  -- JSON array of recorded operations
);

CREATE INDEX idx_change_set_actor_status ON changeset.change_set (actor, status);
