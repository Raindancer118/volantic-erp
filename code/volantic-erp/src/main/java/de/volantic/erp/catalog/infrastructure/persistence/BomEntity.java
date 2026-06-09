package de.volantic.erp.catalog.infrastructure.persistence;

import de.volantic.erp.catalog.domain.model.Bom;
import de.volantic.erp.catalog.domain.model.BomId;
import de.volantic.erp.catalog.domain.model.BomLine;
import de.volantic.erp.catalog.domain.model.ProductId;
import de.volantic.erp.core.AbstractEntity;
import de.volantic.erp.core.measure.Quantity;
import de.volantic.erp.core.measure.UnitOfMeasure;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** JPA representation of a versioned BOM with its lines (collection table). Table {@code catalog.bom}. */
@Entity
@Table(schema = "catalog", name = "bom")
class BomEntity extends AbstractEntity {

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "bom_version", nullable = false, updatable = false)
    private int version;

    @Column(name = "valid_from", updatable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", updatable = false)
    private LocalDate validTo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "catalog", name = "bom_line", joinColumns = @JoinColumn(name = "bom_id"))
    private List<BomLineRow> lines = new ArrayList<>();

    protected BomEntity() {
    }

    private BomEntity(Bom bom) {
        super(bom.id().value());
        this.productId = bom.productId().value();
        this.version = bom.version();
        this.validFrom = bom.validFrom();
        this.validTo = bom.validTo();
        for (BomLine line : bom.lines()) {
            this.lines.add(new BomLineRow(
                    line.componentId().value(), line.quantity().amount(), line.quantity().unit().code()));
        }
    }

    static BomEntity from(Bom bom) {
        return new BomEntity(bom);
    }

    Bom toDomain() {
        List<BomLine> domainLines = lines.stream()
                .map(row -> new BomLine(
                        new ProductId(row.componentId()),
                        Quantity.of(row.qtyAmount(), new UnitOfMeasure(row.qtyUnit()))))
                .toList();
        return Bom.reconstitute(new BomId(getId()), new ProductId(productId), version, validFrom, validTo, domainLines);
    }
}
