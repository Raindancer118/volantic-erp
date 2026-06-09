package de.volantic.erp.core.entitylink.application.port.out;

import de.volantic.erp.core.entitylink.EntityLink;
import de.volantic.erp.core.entitylink.EntityRef;

import java.util.List;

/** Outbound port for persisting the entity-link graph. */
public interface EntityLinkStore {

    /** Adds the edge if it does not already exist (idempotent). */
    void add(EntityLink link);

    /** Removes the edge if present. */
    void remove(EntityLink link);

    List<EntityLink> findOutgoing(EntityRef from);

    List<EntityLink> findIncoming(EntityRef to);
}
