package de.volantic.erp.core.entitylink.infrastructure.persistence;

import de.volantic.erp.core.entitylink.EntityLink;
import de.volantic.erp.core.entitylink.EntityRef;
import de.volantic.erp.core.entitylink.application.port.out.EntityLinkStore;
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
        if (!exists) {
            jpa.save(new EntityLinkEntity(
                    link.from().type(), link.from().id(),
                    link.to().type(), link.to().id(), link.linkType()));
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
