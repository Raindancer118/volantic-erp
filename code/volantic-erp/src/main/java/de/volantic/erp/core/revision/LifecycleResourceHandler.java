package de.volantic.erp.core.revision;

import java.util.Map;
import java.util.UUID;

/**
 * Optional SPI a business module provides for resources that support bulk <em>creation</em> and
 * <em>deletion</em> inside a change-set (ADR-0006 §2), on top of the field-update {@link BulkEditHandler}.
 * Every operation goes through the module's own domain service — never a direct DB write — so validation,
 * hooks and the audit trail run.
 *
 * <p>The Rollback Engine compensates these forward-only (it never deletes audit history):
 * <ul>
 *   <li>a bulk-created resource is taken back by {@link #delete(UUID) deleting} it;</li>
 *   <li>a bulk-deleted resource is taken back by {@link #recreate(UUID, String) re-creating} it from the
 *       snapshot captured before the delete — crucially with its <em>original id</em>, so existing links
 *       and references stay valid.</li>
 * </ul>
 */
public interface LifecycleResourceHandler {

    /** The module-qualified resource type this handler serves, e.g. {@code "crm.contact"}. */
    String resourceType();

    /** Creates a resource from a field map through the domain service; returns its new id. */
    UUID create(Map<String, String> data);

    /** A full serializable snapshot of the resource for later re-creation, or {@code null} if it is absent. */
    String snapshot(UUID id);

    /** Deletes the resource through the domain service (used directly and to compensate a CREATE). */
    void delete(UUID id);

    /** Re-creates a previously deleted resource with its original id from {@link #snapshot(UUID) snapshot}. */
    void recreate(UUID id, String snapshot);
}
