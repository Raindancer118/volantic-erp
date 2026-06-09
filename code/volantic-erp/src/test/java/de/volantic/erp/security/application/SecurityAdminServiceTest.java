package de.volantic.erp.security.application;

import de.volantic.erp.audit.AuditTrail;
import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.application.port.out.SecurityWriteStore;
import de.volantic.erp.security.domain.model.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Use-case orchestration of the write side over the mocked {@link SecurityWriteStore} port. */
class SecurityAdminServiceTest {

    private final SecurityWriteStore store = mock(SecurityWriteStore.class);
    private final AuditTrail auditTrail = mock(AuditTrail.class);
    private final SecurityAdminService service = new SecurityAdminService(store, auditTrail);

    @Test
    void provisionUserCreatesWhenAbsent() {
        when(store.userExists("sub-1")).thenReturn(false);

        service.provisionUser("sub-1", "m.muster", "m@example.de");

        verify(store).createUser("sub-1", "m.muster", "m@example.de");
    }

    @Test
    void provisionUserIsIdempotentWhenPresent() {
        when(store.userExists("sub-1")).thenReturn(true);

        service.provisionUser("sub-1", "m.muster", "m@example.de");

        verify(store, never()).createUser("sub-1", "m.muster", "m@example.de");
    }

    @Test
    void delegatesDefinitionsAndAssignments() {
        service.definePermission("hr.salary:read", "Read salary");
        service.defineRole("hr-mgr", "HR Manager", Set.of("hr.salary:read"));
        service.setUserStatus("sub-1", UserStatus.DISABLED);
        service.assignRole("sub-1", "hr-mgr", AccessScope.GLOBAL);

        verify(store).upsertPermission("hr.salary:read", "Read salary");
        verify(store).upsertRole("hr-mgr", "HR Manager", Set.of("hr.salary:read"));
        verify(store).setUserStatus("sub-1", UserStatus.DISABLED);
        verify(store).assignRole("sub-1", "hr-mgr", AccessScope.GLOBAL);
    }

    @Test
    void recordsAuditOnAccessControlChanges() {
        when(store.userExists("sub-1")).thenReturn(false);

        service.provisionUser("sub-1", "m.muster", "m@example.de");
        service.setUserStatus("sub-1", UserStatus.DISABLED);
        service.assignRole("sub-1", "hr-mgr", AccessScope.GLOBAL);

        verify(auditTrail).record(eq("security.user-provisioned"), eq("security.user"), isNull(), any());
        verify(auditTrail).record(eq("security.user-status-changed"), eq("security.user"), isNull(), any());
        verify(auditTrail).record(eq("security.role-assigned"), eq("security.user"), isNull(), any());
    }
}
