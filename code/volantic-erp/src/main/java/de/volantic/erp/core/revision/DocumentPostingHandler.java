package de.volantic.erp.core.revision;

import java.util.UUID;

/**
 * Optional SPI a business module provides so its <strong>posted documents (Belege)</strong> can take part
 * in a change-set (ADR-0006 §2). Posting a legal document (e.g. an invoice) draws a gap-free GoBD number
 * and is immutable, so it cannot be compensated by a delete or a field restore — the Rollback Engine
 * compensates it forward-only with a <strong>storno</strong> (a cancellation document in the same number
 * range; the original is never altered or deleted).
 *
 * <p>A change-set bulk-posts <em>existing drafts</em> (created through the module's normal API) so the
 * whole batch is one reversible session — mass invoicing being the canonical case. Both operations go
 * through the module's own domain service (authorization, audit), never a direct DB write.
 */
public interface DocumentPostingHandler {

    /** The module-qualified document type this handler serves, e.g. {@code "sales.invoice"}. */
    String resourceType();

    /** Posts the existing draft document with this id (assigns its gap-free number, freezes it). */
    void post(UUID id);

    /** Compensates a posting forward-only by cancelling the document via a storno in the same range. */
    void storno(UUID id);
}
