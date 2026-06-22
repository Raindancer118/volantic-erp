package de.volantic.erp.security.domain.model;

import de.volantic.erp.core.UuidV7;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure-domain invariants of {@link OrgUnit}. */
class OrgUnitTest {

    @Test
    void createWithoutParentIsARoot() {
        OrgUnit unit = OrgUnit.create("ROOT", "Organization", null);

        assertThat(unit.isRoot()).isTrue();
        assertThat(unit.parentId()).isNull();
        assertThat(unit.version()).isNull();
        assertThat(unit.code()).isEqualTo("ROOT");
    }

    @Test
    void createWithParentIsNotARoot() {
        OrgUnitId parent = new OrgUnitId(UuidV7.randomUuid());
        OrgUnit unit = OrgUnit.create("BERLIN", "Berlin site", parent);

        assertThat(unit.isRoot()).isFalse();
        assertThat(unit.parentId()).isEqualTo(parent);
    }

    @Test
    void blankCodeOrNameIsRejected() {
        assertThatThrownBy(() -> OrgUnit.create(" ", "name", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OrgUnit.create("CODE", " ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aUnitCannotBeItsOwnParent() {
        OrgUnitId id = new OrgUnitId(UuidV7.randomUuid());
        assertThatThrownBy(() -> OrgUnit.reconstitute(id, 0, id, "CODE", "name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own parent");
    }

    @Test
    void renameChangesOnlyTheName() {
        OrgUnit unit = OrgUnit.create("CODE", "old", null);
        unit.rename("new");

        assertThat(unit.name()).isEqualTo("new");
        assertThat(unit.code()).isEqualTo("CODE");
    }

    @Test
    void identityEqualityByIdOnly() {
        OrgUnitId id = new OrgUnitId(UuidV7.randomUuid());
        OrgUnit a = OrgUnit.reconstitute(id, 1, null, "CODE", "name-a");
        OrgUnit b = OrgUnit.reconstitute(id, 2, null, "CODE", "name-b");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
