package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.core.UuidV7;
import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.domain.model.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Maps the cached {@link CachedUser} snapshot back onto the pure domain {@code User} — no Spring, no DB. */
class UserDirectoryAdapterMappingTest {

    private final UserGraphCache cache = mock(UserGraphCache.class);
    private final UserDirectoryAdapter adapter = new UserDirectoryAdapter(cache);

    @Test
    void mapsGlobalActiveUser() {
        when(cache.load("sub-1")).thenReturn(new CachedUser("sub-1", "ACTIVE",
                List.of(new CachedUser.CachedAssignment("hr-mgr", Set.of("hr.salary:read"), null, null))));

        Optional<User> user = adapter.findByOidcSubject("sub-1");

        assertThat(user).isPresent();
        assertThat(user.get().isPermitted("hr.salary:read", AccessScope.GLOBAL)).isTrue();
        assertThat(user.get().permissionKeys()).containsExactly("hr.salary:read");
    }

    @Test
    void mapsScopedAssignment() {
        UUID dept = UuidV7.randomUuid();
        when(cache.load("sub-1")).thenReturn(new CachedUser("sub-1", "ACTIVE",
                List.of(new CachedUser.CachedAssignment("hr-mgr", Set.of("hr.salary:read"), "DEPT", dept))));

        User user = adapter.findByOidcSubject("sub-1").orElseThrow();

        assertThat(user.isPermitted("hr.salary:read", AccessScope.of("DEPT", dept))).isTrue();
        assertThat(user.isPermitted("hr.salary:read", AccessScope.GLOBAL)).isFalse();
    }

    @Test
    void disabledSnapshotYieldsNoPermissions() {
        when(cache.load("sub-1")).thenReturn(new CachedUser("sub-1", "DISABLED",
                List.of(new CachedUser.CachedAssignment("hr-mgr", Set.of("hr.salary:read"), null, null))));

        User user = adapter.findByOidcSubject("sub-1").orElseThrow();

        assertThat(user.isPermitted("hr.salary:read", AccessScope.GLOBAL)).isFalse();
        assertThat(user.permissionKeys()).isEmpty();
    }

    @Test
    void unknownSubjectMapsToEmpty() {
        when(cache.load("ghost")).thenReturn(null);

        assertThat(adapter.findByOidcSubject("ghost")).isEmpty();
    }
}
