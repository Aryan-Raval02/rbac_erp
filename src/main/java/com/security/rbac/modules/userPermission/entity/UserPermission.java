package com.security.rbac.modules.userPermission.entity;

import com.security.rbac.modules.action.entity.Action;
import com.security.rbac.modules.module.entity.Module;
import com.security.rbac.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * User-specific permission override stored in the tenant's
 * {@code user_permissions} table.
 *
 * <p>
 * No {@code schema = "..."} — Hibernate targets the active tenant schema via
 * {@code search_path} set by {@code MultiTenantConnectionProviderImpl}.
 *
 * <p>
 * Maps exactly to {@code V4__user_permission.sql}.
 *
 * <h2>Purpose</h2>
 * A row here overrides the role-level permission for a specific user:
 * <ul>
 * <li>{@code allowed = true} — explicitly grants the action on the module</li>
 * <li>{@code allowed = false} — explicitly denies the action on the module</li>
 * </ul>
 *
 * <p>
 * The unique constraint {@code uk_up_user_module_action} on
 * {@code (user_id, module_id, action_id)} prevents duplicate overrides.
 */
@Entity
@Table(name = "user_permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_up_user_module_action", columnNames = { "user_id", "module_id", "action_id" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user this override applies to — FK → users(id) ON DELETE CASCADE. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_up_user"))
    private User user;

    /** The module this override covers — FK → modules(id) ON DELETE CASCADE. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false, foreignKey = @ForeignKey(name = "fk_up_module"))
    private Module module;

    /** The action being overridden — FK → actions(id) ON DELETE CASCADE. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action_id", nullable = false, foreignKey = @ForeignKey(name = "fk_up_action"))
    private Action action;

    /**
     * {@code true} = explicitly granted for this user.
     * {@code false} = explicitly denied for this user.
     */
    @Column(nullable = false)
    private Boolean allowed;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
