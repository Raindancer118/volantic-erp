package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.core.AbstractEntity;
import de.volantic.erp.security.AccessScope;
import de.volantic.erp.security.domain.model.UserStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * JPA representation of the mirrored user. Carries <strong>no password</strong> — only the OIDC
 * subject reference to Authentik. Table {@code security.app_user}.
 */
@Entity
@Table(schema = "security", name = "app_user")
class AppUserEntity extends AbstractEntity {

    @Column(name = "oidc_subject", nullable = false, unique = true)
    private String oidcSubject;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UserRoleEntity> roleAssignments = new HashSet<>();

    protected AppUserEntity() {
    }

    AppUserEntity(String oidcSubject, String username, String email) {
        super(true);
        this.oidcSubject = oidcSubject;
        this.username = username;
        this.email = email;
    }

    void assignRole(RoleEntity role, AccessScope scope) {
        roleAssignments.add(new UserRoleEntity(this, role, scope));
    }

    String oidcSubject() {
        return oidcSubject;
    }

    UserStatus status() {
        return status;
    }

    Set<UserRoleEntity> roleAssignments() {
        return roleAssignments;
    }
}
