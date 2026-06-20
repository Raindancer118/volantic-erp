/**
 * <strong>Sales</strong> — sales documents (Belege), starting with the {@code Invoice} (DB architecture
 * §5.1). An invoice is a draft until posted; posting draws a gap-free GoBD number and freezes it. A posted
 * invoice is immutable and is corrected only by a <em>storno</em> (a negated cancellation document posted
 * into the same number range) — never edited or deleted (ADR-0006 §2). This is the document model the
 * change-set Rollback Engine's storno path builds on.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Sales")
package de.volantic.erp.sales;
