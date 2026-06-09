package de.volantic.erp.catalog.application.port.out;

import de.volantic.erp.catalog.domain.model.Bom;
import de.volantic.erp.catalog.domain.model.BomId;
import de.volantic.erp.catalog.domain.model.ProductId;

import java.util.List;
import java.util.Optional;

/** Outbound port for bill-of-materials persistence. */
public interface BomRepository {

    Bom save(Bom bom);

    Optional<Bom> findById(BomId id);

    boolean existsByProductIdAndVersion(ProductId productId, int version);

    List<Bom> findByProductId(ProductId productId);
}
