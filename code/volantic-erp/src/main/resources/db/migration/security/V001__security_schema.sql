-- IAM / Security-Schema (ADR-0004, DB-Architektur §6).
-- Authentifizierung liegt bei Authentik (OIDC) — hier nur Identitäts-Spiegel + RBAC.

CREATE SCHEMA IF NOT EXISTS security;

CREATE TABLE security.permission (
    id             UUID         NOT NULL PRIMARY KEY,
    version        BIGINT       NOT NULL,
    permission_key VARCHAR(150) NOT NULL UNIQUE,   -- resource:action, z. B. hr.salary:read
    description    VARCHAR(500)
);

CREATE TABLE security.role (
    id          UUID         NOT NULL PRIMARY KEY,
    version     BIGINT       NOT NULL,
    role_key    VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(500)
);

CREATE TABLE security.role_permission (
    role_id       UUID NOT NULL REFERENCES security.role (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES security.permission (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE security.app_user (
    id           UUID         NOT NULL PRIMARY KEY,
    version      BIGINT       NOT NULL,
    oidc_subject VARCHAR(255) NOT NULL UNIQUE,     -- Bezug zum Authentik-Subject; kein Passwort
    username     VARCHAR(150) NOT NULL,
    email        VARCHAR(320),
    status       VARCHAR(20)  NOT NULL
);

CREATE TABLE security.user_role (
    id         UUID   NOT NULL PRIMARY KEY,
    version    BIGINT NOT NULL,
    user_id    UUID   NOT NULL REFERENCES security.app_user (id) ON DELETE CASCADE,
    role_id    UUID   NOT NULL REFERENCES security.role (id),
    scope_type VARCHAR(50),                         -- NULL = global; sonst Instanz-/Bereichs-Scope
    scope_id   UUID
);

CREATE INDEX idx_user_role_user ON security.user_role (user_id);
CREATE INDEX idx_user_role_scope ON security.user_role (scope_type, scope_id);
