package de.volantic.erp.changeset.domain.model;

import de.volantic.erp.core.entitylink.EntityRef;
import de.volantic.erp.core.revision.ChangeOperation;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure domain tests for the {@link ChangeSet} aggregate: recording, ordering, and lifecycle invariants. */
class ChangeSetTest {

    private static RecordedOperation update() {
        return new RecordedOperation(
                EntityRef.of("crm.customer", UUID.randomUUID()),
                ChangeOperation.UPDATE, "{\"name\":\"old\"}", "{\"name\":\"new\"}", OffsetDateTime.now());
    }

    @Test
    void opensEmptyAndOpenInTheGivenMode() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.DEFERRED);

        assertThat(session.status()).isEqualTo(ChangeSetStatus.OPEN);
        assertThat(session.mode()).isEqualTo(ChangeSetMode.DEFERRED);
        assertThat(session.actor()).isEqualTo("alice");
        assertThat(session.operations()).isEmpty();
        assertThat(session.closedAt()).isNull();
    }

    @Test
    void recordsOperationsInOrderAndReversesForCompensation() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.LIVE);
        RecordedOperation first = update();
        RecordedOperation second = update();

        session.record(first);
        session.record(second);

        assertThat(session.operations()).containsExactly(first, second);
        assertThat(session.operationsForReversal()).containsExactly(second, first);
    }

    @Test
    void exposedOperationListsAreUnmodifiable() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.LIVE);
        session.record(update());

        assertThatThrownBy(() -> session.operations().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void cannotRecordAfterTheSessionIsClosed() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.LIVE);
        session.commit();

        assertThatThrownBy(() -> session.record(update()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMMITTED");
    }

    @Test
    void commitClosesTheSessionOnceOnly() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.DEFERRED);

        session.commit();

        assertThat(session.status()).isEqualTo(ChangeSetStatus.COMMITTED);
        assertThat(session.closedAt()).isNotNull();
        assertThatThrownBy(session::commit).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void liveSessionCanBeRevertedWhileOpen() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.LIVE);

        session.revert();

        assertThat(session.status()).isEqualTo(ChangeSetStatus.REVERTED);
        assertThat(session.closedAt()).isNotNull();
    }

    @Test
    void committedSessionCanStillBeReverted() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.DEFERRED);
        session.commit();

        session.revert();

        assertThat(session.status()).isEqualTo(ChangeSetStatus.REVERTED);
    }

    @Test
    void openProbemodusSessionCannotBeRevertedButDiscarded() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.DEFERRED);

        assertThatThrownBy(session::revert)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("discarded");

        session.discard();
        assertThat(session.status()).isEqualTo(ChangeSetStatus.DISCARDED);
    }

    @Test
    void liveSessionCannotBeDiscarded() {
        ChangeSet session = ChangeSet.open("alice", ChangeSetMode.LIVE);

        assertThatThrownBy(session::discard)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Probemodus");
    }

    @Test
    void createOperationMustNotCarryABeforeState() {
        assertThatThrownBy(() -> new RecordedOperation(
                EntityRef.of("crm.customer", UUID.randomUUID()),
                ChangeOperation.CREATE, "{\"some\":\"state\"}", null, OffsetDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CREATE");
    }
}
