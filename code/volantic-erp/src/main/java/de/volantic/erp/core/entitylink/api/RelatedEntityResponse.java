package de.volantic.erp.core.entitylink.api;

import de.volantic.erp.core.entitylink.EntityLink;

/** One related entity in a 360° view: the link type plus the target entity (REST v1). */
public record RelatedEntityResponse(String linkType, String entityType, String entityId) {

    static RelatedEntityResponse fromOutgoing(EntityLink link) {
        return new RelatedEntityResponse(link.linkType(), link.to().type(), link.to().id().toString());
    }

    static RelatedEntityResponse fromIncoming(EntityLink link) {
        return new RelatedEntityResponse(link.linkType(), link.from().type(), link.from().id().toString());
    }
}
