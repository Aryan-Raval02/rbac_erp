package com.security.rbac.modules.rolePermission.repo;

import com.security.rbac.modules.rolePermission.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link RolePermission} —
 * always accesses {@code public.platform_role_permissions}.
 */
@Repository
public interface RolePermissionRepository
        extends JpaRepository<RolePermission, Long> {

    /**
     * Find all permissions for a specific role.
     * Used for loading the full role permission matrix.
     */
    List<RolePermission> findByRoleId(Long roleId);

    /**
     * Find all allowed permissions for a role.
     * Used for security/authorization checks.
     */
    List<RolePermission> findByRoleIdAndAllowedTrue(Long roleId);

    /**
     * Find an exact role → module → action mapping.
     * Used to check before inserting (unique constraint guard).
     */
    Optional<RolePermission> findByRoleIdAndModuleIdAndActionId(
            Long roleId, Long moduleId, Long actionId);

    /**
     * Check if a role is allowed to perform an action on a module.
     * Efficient for authorization checks — avoids loading the full entity.
     */
    @Query("""
            SELECT COUNT(p) > 0
            FROM RolePermission p
            WHERE p.role.id     = :roleId
              AND p.module.id   = :moduleId
              AND p.action.id   = :actionId
              AND p.allowed     = true
            """)
    boolean isAllowed(
            @Param("roleId") Long roleId,
            @Param("moduleId") Long moduleId,
            @Param("actionId") Long actionId);

    /**
     * Delete all permissions for a role — used when a role is removed.
     */
    void deleteByRoleId(Long roleId);
}
