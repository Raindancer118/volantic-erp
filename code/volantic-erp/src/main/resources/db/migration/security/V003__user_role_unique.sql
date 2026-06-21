-- A user must not hold the same role at the same scope twice. Without this the application Set could not
-- deduplicate (UserRoleEntity has an id-based identity with a fresh id per instance), so concurrent or
-- repeated assignments produced duplicate rows. Dedup any existing duplicates, then enforce uniqueness.
--
-- scope_type/scope_id are nullable (NULL = global). NULLS NOT DISTINCT (PostgreSQL 15+) makes two global
-- assignments for the same (user, role) collide as intended, instead of being treated as distinct.

DELETE FROM security.user_role a
      USING security.user_role b
      WHERE a.ctid < b.ctid
        AND a.user_id = b.user_id
        AND a.role_id = b.role_id
        AND a.scope_type IS NOT DISTINCT FROM b.scope_type
        AND a.scope_id   IS NOT DISTINCT FROM b.scope_id;

ALTER TABLE security.user_role
    ADD CONSTRAINT uq_user_role_assignment
    UNIQUE NULLS NOT DISTINCT (user_id, role_id, scope_type, scope_id);
