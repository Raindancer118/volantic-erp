package de.volantic.erp.catalog.application;

import de.volantic.erp.catalog.domain.model.BomId;
import de.volantic.erp.catalog.domain.model.ProductId;

/** CRM-style error hierarchy for the catalog module, mapped to HTTP status by the api layer. */
public final class CatalogExceptions {

    private CatalogExceptions() {
    }

    /** Mapped to HTTP 404. */
    public abstract static sealed class NotFound extends RuntimeException
            permits ProductNotFound, BomNotFound {
        protected NotFound(String message) {
            super(message);
        }
    }

    /** Mapped to HTTP 409. */
    public abstract static sealed class Conflict extends RuntimeException
            permits SkuAlreadyExists, BomVersionAlreadyExists {
        protected Conflict(String message) {
            super(message);
        }
    }

    public static final class ProductNotFound extends NotFound {
        public ProductNotFound(ProductId id) {
            super("product not found: " + id.value());
        }
    }

    public static final class BomNotFound extends NotFound {
        public BomNotFound(BomId id) {
            super("bom not found: " + id.value());
        }
    }

    public static final class SkuAlreadyExists extends Conflict {
        public SkuAlreadyExists(String sku) {
            super("product sku already exists: " + sku);
        }
    }

    public static final class BomVersionAlreadyExists extends Conflict {
        public BomVersionAlreadyExists(ProductId productId, int version) {
            super("bom version already exists for product " + productId.value() + ": v" + version);
        }
    }
}
