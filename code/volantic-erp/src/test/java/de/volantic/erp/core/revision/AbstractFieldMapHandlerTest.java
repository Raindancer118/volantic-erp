package de.volantic.erp.core.revision;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests of the {@link AbstractFieldMapHandler} template: capture/serialize, partial overlay on apply,
 * and compensation. Uses a tiny in-memory subclass so the generic behaviour is tested once, independent of
 * any concrete aggregate.
 */
class AbstractFieldMapHandlerTest {

    /** In-memory handler over a {@code field → value} map per id, with {@code email} allowed to be null. */
    private static final class FakeHandler extends AbstractFieldMapHandler {
        private final Map<UUID, Map<String, String>> store = new HashMap<>();

        FakeHandler() {
            super(new ObjectMapper());
        }

        @Override
        public String resourceType() {
            return "test.thing";
        }

        @Override
        public Set<String> editableFields() {
            return Set.of("name", "email");
        }

        @Override
        protected Map<String, String> readFields(UUID id) {
            Map<String, String> current = store.get(id);
            return current == null ? null : new LinkedHashMap<>(current);
        }

        @Override
        protected void writeFields(UUID id, Map<String, String> fields) {
            if (!store.containsKey(id)) {
                throw new IllegalStateException("no such id");
            }
            store.put(id, new LinkedHashMap<>(fields));
        }

        void seed(UUID id, String name, String email) {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("name", name);
            fields.put("email", email);
            store.put(id, fields);
        }

        Map<String, String> current(UUID id) {
            return store.get(id);
        }
    }

    @Test
    void captureReturnsNullForMissingResource() {
        assertThat(new FakeHandler().capture(UUID.randomUUID())).isNull();
    }

    @Test
    void applyOverlaysOnlyTheGivenFieldsAndKeepsTheRest() {
        FakeHandler handler = new FakeHandler();
        UUID id = UUID.randomUUID();
        handler.seed(id, "Acme", "info@acme.de");

        handler.applyChange(id, Map.of("name", "Acme Corp"));

        assertThat(handler.current(id)).containsEntry("name", "Acme Corp").containsEntry("email", "info@acme.de");
    }

    @Test
    void applyOnMissingResourceFails() {
        assertThatThrownBy(() -> new FakeHandler().applyChange(UUID.randomUUID(), Map.of("name", "X")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void captureThenCompensateRestoresExactStateIncludingNulls() {
        FakeHandler handler = new FakeHandler();
        UUID id = UUID.randomUUID();
        handler.seed(id, "Acme", null);

        String before = handler.capture(id);
        handler.applyChange(id, Map.of("name", "Changed", "email", "new@acme.de"));
        assertThat(handler.current(id)).containsEntry("name", "Changed").containsEntry("email", "new@acme.de");

        handler.compensate(ChangeOperation.UPDATE, id, before);

        assertThat(handler.current(id)).containsEntry("name", "Acme");
        assertThat(handler.current(id).get("email")).isNull();
    }

    @Test
    void compensatingCreateOrDeleteIsRejected() {
        FakeHandler handler = new FakeHandler();
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> handler.compensate(ChangeOperation.CREATE, id, null))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> handler.compensate(ChangeOperation.DELETE, id, "{}"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
