package de.volantic.erp.security.infrastructure.persistence;

import de.volantic.erp.core.AbstractEntity;
import de.volantic.erp.security.AccessScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * JPA representation of a role assignment to a user, optionally restricted to a data scope.
 * Table {@code security.user_role}; {@code scope_type == null} means global.
 */
@Entity
@Table(schema = "security", name = "user_role")
class UserRoleEntity extends AbstractEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUserEntity user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Column(name = "scope_type")
    private String scopeType;

    @Column(name = "scope_id")
    private UUID scopeId;

    protected UserRoleEntity() {
    }

    UserRoleEntity(AppUserEntity user, RoleEntity role, AccessScope scope) {
        super(true);
        this.user = user;
        this.role = role;
        if (scope != null && !scope.isGlobal()) {
            this.scopeType = scope.type();
            this.scopeId = scope.id();
        }
    }

    RoleEntity role() {
        return role;
    }

    /** Reconstructs the domain {@link AccessScope}; a {@code null} scope type ⇒ {@link AccessScope#GLOBAL}. */
    AccessScope scope() {
        return scopeType == null ? AccessScope.GLOBAL : AccessScope.of(scopeType, scopeId);
    }
}
