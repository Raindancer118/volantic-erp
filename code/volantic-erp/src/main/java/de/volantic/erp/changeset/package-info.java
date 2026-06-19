/**
 * <strong>Change Sets</strong> — grouped, reversible edit sessions across all modules (ADR-0006).
 *
 * <p>One {@code ChangeSet} bundles the operations of an actor and runs in one of two modes:
 * <ul>
 *   <li><b>LIVE</b> — changes take effect immediately and can be taken back afterwards via the
 *       <em>Rollback Engine</em> (forward-only, GoBD-safe compensation — never a delete on the audit
 *       trail).</li>
 *   <li><b>DEFERRED</b> (the <em>Probemodus</em>) — must be explicitly activated; nothing is written for
 *       real until "Übertragen" (commit). Discarding leaves no trace.</li>
 * </ul>
 *
 * <p>The module is generic: it drives mass edits and compensation through the {@code core.revision} SPI
 * ({@code BulkEditHandler}, {@code ReversibleResourceHandler}) that each business module provides, so it
 * never depends on module-specific types.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Change Sets")
package de.volantic.erp.changeset;
