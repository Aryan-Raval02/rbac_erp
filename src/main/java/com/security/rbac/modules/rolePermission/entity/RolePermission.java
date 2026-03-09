package com.security.rbac.modules.rolePermission.entity;

import com.security.rbac.modules.action.entity.Action;
import com.security.rbac.modules.module.entity.Module;
import com.security.rbac.modules.role.entity.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Maps a platform role → module → action permission stored in
 * {@code public.platform_role_permissions}.
 *
 * <p>
 * Created by V3 migration. A row says: "Role X is [allowed/denied] to
 * perform Action Y on Module Z at the platform level."
 *
 * <p>
 * The unique constraint {@code uk_prp_role_module_action} on
 * (role_id, module_id, action_id) prevents duplicate mappings.
 */
@Entity
@Table(name = "role_permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_prp_role_module_action", columnNames = { "role_id", "module_id", "action_id" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The platform role this permission applies to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prp_role"))
    private Role role;

    /** The platform module this permission covers. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prp_module"))
    private Module module;

    /** The specific action being permitted or denied. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prp_action"))
    private Action action;

    /**
     * {@code true} = the role is allowed this action on this module.
     * {@code false} = explicitly denied (useful for deny-override policies).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean allowed = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
