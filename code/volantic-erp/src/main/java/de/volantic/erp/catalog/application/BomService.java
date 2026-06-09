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
import java.util.List;

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
        if (boms.existsByProductIdAndVersion(productId, version)) {
            throw new CatalogExceptions.BomVersionAlreadyExists(productId, version);
        }
        return boms.save(Bom.create(productId, version, validFrom, validTo, lines));
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
