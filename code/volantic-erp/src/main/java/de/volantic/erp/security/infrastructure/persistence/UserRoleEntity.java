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
 * JPA-Abbild der Rollenzuweisung an einen Nutzer, optional auf einen Daten-Scope eingeschränkt.
 * Tabelle {@code security.user_role}; {@code scope_type == null} bedeutet global.
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

    /** Rekonstruiert den Domänen-{@link AccessScope}; {@code null}-Scope-Typ ⇒ {@link AccessScope#GLOBAL}. */
    AccessScope scope() {
        return scopeType == null ? AccessScope.GLOBAL : AccessScope.of(scopeType, scopeId);
    }
}
