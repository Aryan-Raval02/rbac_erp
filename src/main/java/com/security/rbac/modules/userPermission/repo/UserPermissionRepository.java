package com.security.rbac.modules.userPermission.repo;

import com.security.rbac.modules.userPermission.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link UserPermission} — targets the active tenant schema via
 * search_path.
 */
@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    /**
     * All permission overrides for a specific user.
     */
    List<UserPermission> findByUserId(Long userId);

    /**
     * Only the allowed=true overrides for a user (for authorization checks).
     */
    @Query("""
                select up
                from UserPermission up
                join fetch up.module m
                join fetch up.action a
                where up.user.id = :userId
                  and up.allowed = true
            """)
    List<UserPermission> findByUserIdAndAllowedTrue(@Param("userId") Long userId);

    /**
     * Exact lookup — used as a duplicate guard before inserting.
     */
    Optional<UserPermission> findByUserIdAndModuleIdAndActionId(
            Long userId, Long moduleId, Long actionId);

    /**
     * Efficient authorization check — returns true if user has an explicit
     * allow override for a specific module + action.
     */
    @Query("""
            SELECT COUNT(p) > 0
            FROM UserPermission p
            WHERE p.user.id   = :userId
              AND p.module.id = :moduleId
              AND p.action.id = :actionId
              AND p.allowed   = true
            """)
    boolean isAllowed(
            @Param("userId") Long userId,
            @Param("moduleId") Long moduleId,
            @Param("actionId") Long actionId);

    /**
     * Remove all overrides for a user — called when a user is deleted.
     */
    void deleteByUserId(Long userId);
}
