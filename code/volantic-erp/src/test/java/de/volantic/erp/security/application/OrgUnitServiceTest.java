package de.volantic.erp.security.application;

import de.volantic.erp.audit.AuditTrail;
import de.volantic.erp.core.UuidV7;
import de.volantic.erp.security.application.port.out.OrgUnitRepository;
import de.volantic.erp.security.domain.model.OrgUnit;
import de.volantic.erp.security.domain.model.OrgUnitId;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Use-case logic of {@link OrgUnitService} over a mocked repository (no Spring/DB). */
class OrgUnitServiceTest {

    private final OrgUnitRepository repository = mock(OrgUnitRepository.class);
    private final AuditTrail audit = mock(AuditTrail.class);
    private final OrgUnitService service = new OrgUnitService(repository, audit);

    @Test
    void createRootPersistsWhenCodeIsFree() {
        when(repository.existsByCode("ROOT")).thenReturn(false);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        OrgUnit created = service.createOrgUnit("ROOT", "Organization", null);

        assertThat(created.code()).isEqualTo("ROOT");
        assertThat(created.isRoot()).isTrue();
        verify(repository).save(any(OrgUnit.class));
        verify(audit).record(any(), any(), any(), any());
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.existsByCode("ROOT")).thenReturn(true);

        assertThatThrownBy(() -> service.createOrgUnit("ROOT", "Organization", null))
                .isInstanceOf(OrgUnitCodeAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsUnknownParent() {
        OrgUnitId parent = new OrgUnitId(UuidV7.randomUuid());
        when(repository.existsByCode("BERLIN")).thenReturn(false);
        when(repository.existsById(parent)).thenReturn(false);

        assertThatThrownBy(() -> service.createOrgUnit("BERLIN", "Berlin", parent))
                .isInstanceOf(OrgUnitNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void getThrowsWhenMissing() {
        OrgUnitId id = new OrgUnitId(UuidV7.randomUuid());
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrgUnit(id)).isInstanceOf(OrgUnitNotFoundException.class);
    }

    @Test
    void renameRejectsStaleVersion() {
        OrgUnit existing = OrgUnit.reconstitute(new OrgUnitId(UuidV7.randomUuid()), 3, null, "ROOT", "old");
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.renameOrgUnit(existing.id(), 1, "new"))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void renameMutatesAndSavesOnMatchingVersion() {
        OrgUnit existing = OrgUnit.reconstitute(new OrgUnitId(UuidV7.randomUuid()), 3, null, "ROOT", "old");
        when(repository.findById(existing.id())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        OrgUnit renamed = service.renameOrgUnit(existing.id(), 3, "new");

        assertThat(renamed.name()).isEqualTo("new");
        verify(repository).save(existing);
    }
}
