package de.volantic.erp.core.entitylink;

import java.util.List;

/**
 * Public API of the entity-link graph — the 360° foundation (DB architecture §5.3). Other modules
 * record relationships here (idempotently) and query them for the object-centric cockpit. Kept
 * generic: it knows only {@link EntityRef}s and string link types, never module-specific concepts.
 *
 * <p>Links are normally maintained via domain events (e.g. the crm module records
 * {@code crm.customer HAS_CONTACT crm.contact} when a contact is added), so the graph stays consistent
 * without callers coupling to each other.
 */
public interface EntityLinkRegistry {

    /** Records a directed link (idempotent: an identical edge is not duplicated). */
    void link(EntityRef from, EntityRef to, String linkType);

    /** Removes a directed link if present (no-op otherwise). */
    void unlink(EntityRef from, EntityRef to, String linkType);

    /** All outgoing links from the given entity — "show me everything related to X". */
    List<EntityLink> outgoing(EntityRef from);

    /** All incoming links to the given entity. */
    List<EntityLink> incoming(EntityRef to);
}
