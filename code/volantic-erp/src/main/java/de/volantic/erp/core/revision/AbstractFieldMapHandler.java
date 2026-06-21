package de.volantic.erp.core.revision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Template base for a resource that takes part in mass edits and the Rollback Engine (ADR-0006) by
 * representing its bulk-editable state as a flat {@code field → value} map. It implements the generic
 * {@code core.revision} contract once — capture/serialize, partial-overlay apply, and compensation — so a
 * concrete handler only has to read and write the fields of one aggregate through that aggregate's own
 * domain service. Subclasses live in their module's {@code infrastructure.revision} package.
 *
 * <p>Both handler roles are combined here because, per resource, they share the same field model and the
 * same serialization. State is exchanged as JSON over the opaque {@code String} the change-set domain
 * stores; the domain never interprets it.
 *
 * <p>Only {@link ChangeOperation#UPDATE} is supported: mass edits change fields of existing resources, and
 * their reversal restores the captured field values. Bulk {@code CREATE}/{@code DELETE} is intentionally
 * out of scope for this SPI (ADR-0006) and is rejected loudly rather than half-implemented.
 */
public abstract class AbstractFieldMapHandler implements BulkEditHandler, ReversibleResourceHandler {

    private static final TypeReference<LinkedHashMap<String, String>> FIELD_MAP = new TypeReference<>() {
    };

    private final ObjectMapper json;

    protected AbstractFieldMapHandler(ObjectMapper json) {
        this.json = json;
    }

    /**
     * Reads the current values of all {@link #editableFields()} for the resource, or {@code null} if the
     * resource does not exist. Implemented against the module's domain service (never a direct DB read).
     */
    protected abstract Map<String, String> readFields(UUID id);

    /**
     * Writes the given full set of editable field values to the resource through the module's domain
     * service (so validation, hooks and the audit trail run). The map always contains every
     * {@link #editableFields() editable field}.
     */
    protected abstract void writeFields(UUID id, Map<String, String> fields);

    @Override
    public String capture(UUID id) {
        Map<String, String> fields = readFields(id);
        return fields == null ? null : serialize(fields);
    }

    @Override
    public void applyChange(UUID id, Map<String, String> fieldChanges) {
        Map<String, String> current = readFields(id);
        if (current == null) {
            throw new IllegalStateException(resourceType() + " " + id + " does not exist");
        }
        // Overlay only the requested changes so untouched editable fields keep their current value — a
        // single-field bulk edit must not blank the others.
        Map<String, String> merged = new LinkedHashMap<>(current);
        merged.putAll(fieldChanges);
        writeFields(id, merged);
    }

    @Override
    public void compensate(ChangeOperation operation, UUID id, String beforeState) {
        if (operation != ChangeOperation.UPDATE) {
            throw new UnsupportedOperationException(
                    "compensation of " + operation + " is not supported for " + resourceType()
                            + " (mass edits are field updates only, ADR-0006)");
        }
        writeFields(id, deserialize(beforeState));
    }

    private String serialize(Map<String, String> fields) {
        try {
            return json.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialize fields for " + resourceType(), e);
        }
    }

    private Map<String, String> deserialize(String state) {
        try {
            return json.readValue(state, FIELD_MAP);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not deserialize fields for " + resourceType(), e);
        }
    }
}
