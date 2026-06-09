package de.volantic.erp.core.entitylink.application;

import de.volantic.erp.core.entitylink.EntityLink;
import de.volantic.erp.core.entitylink.EntityLinkRegistry;
import de.volantic.erp.core.entitylink.EntityRef;
import de.volantic.erp.core.entitylink.application.port.out.EntityLinkStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implements the {@link EntityLinkRegistry}: orchestration only, delegating persistence to the
 * {@link EntityLinkStore} outbound port. Idempotency of {@code link} is guaranteed by the store
 * (and the DB unique constraint).
 */
@Service
class EntityLinkService implements EntityLinkRegistry {

    private final EntityLinkStore store;

    EntityLinkService(EntityLinkStore store) {
        this.store = store;
    }

    @Override
    @Transactional
    public void link(EntityRef from, EntityRef to, String linkType) {
        store.add(new EntityLink(from, to, linkType));
    }

    @Override
    @Transactional
    public void unlink(EntityRef from, EntityRef to, String linkType) {
        store.remove(new EntityLink(from, to, linkType));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityLink> outgoing(EntityRef from) {
        return store.findOutgoing(from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityLink> incoming(EntityRef to) {
        return store.findIncoming(to);
    }
}
