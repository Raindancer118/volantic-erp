package de.volantic.erp.catalog.application;

import de.volantic.erp.catalog.application.port.out.BomRepository;
import de.volantic.erp.catalog.application.port.out.ProductRepository;
import de.volantic.erp.catalog.domain.model.Bom;
import de.volantic.erp.catalog.domain.model.BomId;
import de.volantic.erp.catalog.domain.model.BomLine;
import de.volantic.erp.catalog.domain.model.ProductId;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bill-of-materials use cases. BOMs are versioned and immutable; creating one validates that the
 * product and every component product exist (referential integrity is kept by the application, since
 * components are referenced by id). Authorization enforced at the service boundary (ADR-0004).
 */
@Service
public class BomService {

    private final BomRepository boms;
    private final ProductRepository products;

    BomService(BomRepository boms, ProductRepository products) {
        this.boms = boms;
        this.products = products;
    }

    @Transactional
    @PreAuthorize("hasPermission(null, 'catalog.bom:write')")
    public Bom createBom(ProductId productId, int version, LocalDate validFrom, LocalDate validTo, List<BomLine> lines) {
        if (!products.existsById(productId)) {
            throw new CatalogExceptions.ProductNotFound(productId);
        }
        for (BomLine line : lines) {
            if (!products.existsById(line.componentId())) {
                throw new CatalogExceptions.ProductNotFound(line.componentId());
            }
        }
        rejectCycles(productId, lines);
        if (boms.existsByProductIdAndVersion(productId, version)) {
            throw new CatalogExceptions.BomVersionAlreadyExists(productId, version);
        }
        return boms.save(Bom.create(productId, version, validFrom, validTo, lines));
    }

    /**
     * Rejects circular structures: a component that (transitively, via its own BOMs) depends back on the
     * product being built. Without this, Product A → B → A would create an infinite loop in MRP
     * explosion or cost roll-ups. Walks the existing BOM graph breadth-first from the new lines'
     * components; a {@code visited} set both bounds the work and tolerates pre-existing cycles in data.
     */
    private void rejectCycles(ProductId productId, List<BomLine> lines) {
        Set<ProductId> visited = new HashSet<>();
        Deque<ProductId> toVisit = new ArrayDeque<>();
        lines.forEach(line -> toVisit.push(line.componentId()));
        while (!toVisit.isEmpty()) {
            ProductId component = toVisit.pop();
            if (component.equals(productId)) {
                throw new CatalogExceptions.CircularBom(productId, component);
            }
            if (!visited.add(component)) {
                continue;
            }
            for (Bom componentBom : boms.findByProductId(component)) {
                componentBom.lines().forEach(line -> toVisit.push(line.componentId()));
            }
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'catalog.bom:read')")
    public Bom getBom(BomId id) {
        return boms.findById(id).orElseThrow(() -> new CatalogExceptions.BomNotFound(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(null, 'catalog.bom:read')")
    public List<Bom> listBomsOfProduct(ProductId productId) {
        return boms.findByProductId(productId);
    }
}
