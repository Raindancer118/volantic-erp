-- Entity-link graph — the 360° foundation (DB architecture §5.3). A generic, indexed edge table
-- carrying cross-module relationships for the cockpit / universal search. Maintained via domain
-- events. Cross-module references are by (type, uuid) without FK (Modulith discipline).

CREATE TABLE core.entity_link (
    id         UUID         NOT NULL PRIMARY KEY,
    version    BIGINT       NOT NULL,
    from_type  VARCHAR(100) NOT NULL,            -- e.g. 'crm.customer'
    from_id    UUID         NOT NULL,
    to_type    VARCHAR(100) NOT NULL,            -- e.g. 'crm.contact'
    to_id      UUID         NOT NULL,
    link_type  VARCHAR(50)  NOT NULL,            -- e.g. 'HAS_CONTACT', 'HAS_ADDRESS'
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (from_type, from_id, to_type, to_id, link_type)
);

CREATE INDEX idx_entity_link_from ON core.entity_link (from_type, from_id);
CREATE INDEX idx_entity_link_to ON core.entity_link (to_type, to_id);
