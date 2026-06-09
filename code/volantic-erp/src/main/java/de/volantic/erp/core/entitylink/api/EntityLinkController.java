package de.volantic.erp.core.entitylink.api;

import de.volantic.erp.core.entitylink.EntityLinkRegistry;
import de.volantic.erp.core.entitylink.EntityRef;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST v1 read endpoint backing the object-centric 360° cockpit: returns everything related to one
 * entity (e.g. {@code GET /v1/core/entities/crm.customer/{id}/links}). Authorization is required;
 * the generic {@code core.entity:read} permission gates the graph view.
 */
@RestController
@RequestMapping("/v1/core/entities")
class EntityLinkController {

    private final EntityLinkRegistry links;

    EntityLinkController(EntityLinkRegistry links) {
        this.links = links;
    }

    @GetMapping("/{type}/{id}/links")
    @PreAuthorize("hasPermission(null, 'core.entity:read')")
    List<RelatedEntityResponse> related(@PathVariable String type,
                                        @PathVariable UUID id,
                                        @RequestParam(defaultValue = "outgoing") Direction direction) {
        EntityRef ref = EntityRef.of(type, id);
        return switch (direction) {
            case outgoing -> links.outgoing(ref).stream().map(RelatedEntityResponse::fromOutgoing).toList();
            case incoming -> links.incoming(ref).stream().map(RelatedEntityResponse::fromIncoming).toList();
        };
    }

    enum Direction {
        outgoing,
        incoming
    }
}
