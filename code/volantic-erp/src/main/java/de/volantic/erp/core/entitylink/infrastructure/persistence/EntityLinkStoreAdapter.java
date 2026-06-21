package de.volantic.erp.core.entitylink.infrastructure.persistence;

import de.volantic.erp.core.entitylink.EntityLink;
import de.volantic.erp.core.entitylink.EntityRef;
import de.volantic.erp.core.entitylink.application.port.out.EntityLinkStore;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;

/** Outbound adapter for {@link EntityLinkStore}: maps between {@link EntityLink} and JPA. */
@Component
class EntityLinkStoreAdapter implements EntityLinkStore {

    private final EntityLinkJpaRepository jpa;

    EntityLinkStoreAdapter(EntityLinkJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void add(EntityLink link) {
        boolean exists = jpa.findByFromTypeAndFromIdAndToTypeAndToIdAndLinkType(
                link.from().type(), link.from().id(),
                link.to().type(), link.to().id(), link.linkType()).isPresent();
        if (exists) {
            return;
        }
        try {
            jpa.saveAndFlush(new EntityLinkEntity(
                    link.from().type(), link.from().id(),
                    link.to().type(), link.to().id(), link.linkType()));
        } catch (DataIntegrityViolationException concurrentInsert) {
            // Check-then-act races with a concurrent (async outbox) insert of the same edge; the table's
            // UNIQUE constraint is the source of truth and rejected the duplicate. Linking is idempotent,
            // so a losing race is a success — swallow it rather than failing the listener.
        }
    }

    @Override
    public void remove(EntityLink link) {
        jpa.findByFromTypeAndFromIdAndToTypeAndToIdAndLinkType(
                link.from().type(), link.from().id(),
                link.to().type(), link.to().id(), link.linkType()).ifPresent(jpa::delete);
    }

    @Override
    public List<EntityLink> findOutgoing(EntityRef from) {
        return jpa.findByFromTypeAndFromId(from.type(), from.id()).stream().map(this::toDomain).toList();
    }

    @Override
    public List<EntityLink> findIncoming(EntityRef to) {
        return jpa.findByToTypeAndToId(to.type(), to.id()).stream().map(this::toDomain).toList();
    }

    private EntityLink toDomain(EntityLinkEntity entity) {
        return new EntityLink(
                EntityRef.of(entity.fromType(), entity.fromId()),
                EntityRef.of(entity.toType(), entity.toId()),
                entity.linkType());
    }
}
