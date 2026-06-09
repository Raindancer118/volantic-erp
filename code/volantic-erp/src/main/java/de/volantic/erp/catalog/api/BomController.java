package de.volantic.erp.catalog.api;

import de.volantic.erp.catalog.application.BomService;
import de.volantic.erp.catalog.domain.model.Bom;
import de.volantic.erp.catalog.domain.model.BomId;
import de.volantic.erp.catalog.domain.model.BomLine;
import de.volantic.erp.catalog.domain.model.ProductId;
import de.volantic.erp.core.measure.Quantity;
import de.volantic.erp.core.measure.UnitOfMeasure;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST v1 endpoints for the versioned BOMs of a product. Nested under the owning product. Thin adapter:
 * it maps DTOs and delegates to {@link BomService}, which enforces authorization.
 */
@RestController
@RequestMapping("/v1/catalog/products/{productId}/boms")
class BomController {

    private final BomService boms;

    BomController(BomService boms) {
        this.boms = boms;
    }

    @PostMapping
    ResponseEntity<BomResponse> create(@PathVariable UUID productId, @Valid @RequestBody CreateBomRequest request) {
        List<BomLine> lines = request.lines().stream()
                .map(line -> new BomLine(
                        new ProductId(line.componentId()),
                        Quantity.of(line.quantity(), new UnitOfMeasure(line.unit()))))
                .toList();
        Bom created = boms.createBom(
                new ProductId(productId), request.version(), request.validFrom(), request.validTo(), lines);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id().value()).toUri();
        return ResponseEntity.created(location).body(BomResponse.from(created));
    }

    @GetMapping("/{id}")
    BomResponse getById(@PathVariable UUID productId, @PathVariable UUID id) {
        return BomResponse.from(boms.getBom(new BomId(id)));
    }

    @GetMapping
    List<BomResponse> list(@PathVariable UUID productId) {
        return boms.listBomsOfProduct(new ProductId(productId)).stream()
                .map(BomResponse::from)
                .toList();
    }
}
