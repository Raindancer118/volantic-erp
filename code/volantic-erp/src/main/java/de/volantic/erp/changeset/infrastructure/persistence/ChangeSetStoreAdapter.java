package de.volantic.erp.changeset.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.volantic.erp.changeset.application.port.out.ChangeSetStore;
import de.volantic.erp.changeset.domain.model.ChangeSet;
import de.volantic.erp.changeset.domain.model.ChangeSetId;
import de.volantic.erp.changeset.domain.model.RecordedOperation;
import de.volantic.erp.core.entitylink.EntityRef;
import de.volantic.erp.core.revision.ChangeOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound adapter for {@link ChangeSetStore}. Maps the {@link ChangeSet} aggregate to/from its JPA row,
 * serializing the recorded operations as a JSON array (opaque snapshots/payloads stay strings). On save
 * it upserts: an existing row is loaded so optimistic locking and the immutable columns are preserved.
 */
@Component
class ChangeSetStoreAdapter implements ChangeSetStore {

    private static final TypeReference<List<OperationRow>> OPERATION_LIST = new TypeReference<>() {
    };

    private final ChangeSetJpaRepository jpa;
    private final ObjectMapper json;

    ChangeSetStoreAdapter(ChangeSetJpaRepository jpa, ObjectMapper json) {
        this.jpa = jpa;
        this.json = json;
    }

    @Override
    public void save(ChangeSet changeSet) {
        ChangeSetEntity entity = jpa.findById(changeSet.id().value())
                .orElseGet(() -> ChangeSetEntity.forNew(
                        changeSet.id().value(), changeSet.actor(), changeSet.mode(), changeSet.openedAt()));
        entity.setStatus(changeSet.status());
        entity.setClosedAt(changeSet.closedAt());
        entity.setOperations(serialize(changeSet.operations()));
        jpa.save(entity);
    }

    @Override
    public Optional<ChangeSet> findById(ChangeSetId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Page<ChangeSet> findByActor(String actor, Pageable pageable) {
        return jpa.findByActorOrderByOpenedAtDesc(actor, pageable).map(this::toDomain);
    }

    private ChangeSet toDomain(ChangeSetEntity entity) {
        List<RecordedOperation> operations = deserialize(entity.getOperations()).stream()
                .map(row -> new RecordedOperation(
                        EntityRef.of(row.type(), row.id()), row.operation(), row.beforeState(), row.payload(), row.recordedAt()))
                .toList();
        return ChangeSet.reconstitute(new ChangeSetId(entity.getId()), entity.getActor(), entity.getMode(),
                entity.getStatus(), entity.getOpenedAt(), entity.getClosedAt(), operations);
    }

    private String serialize(List<RecordedOperation> operations) {
        List<OperationRow> rows = operations.stream()
                .map(op -> new OperationRow(
                        op.target().type(), op.target().id(), op.operation(), op.beforeState(), op.payload(), op.recordedAt()))
                .toList();
        try {
            return json.writeValueAsString(rows);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialize change-set operations", e);
        }
    }

    private List<OperationRow> deserialize(String operations) {
        try {
            return json.readValue(operations, OPERATION_LIST);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not deserialize change-set operations", e);
        }
    }

    /** JSON shape of one recorded operation inside the {@code operations} column. */
    private record OperationRow(
            String type,
            UUID id,
            ChangeOperation operation,
            String beforeState,
            String payload,
            OffsetDateTime recordedAt) {
    }
}
