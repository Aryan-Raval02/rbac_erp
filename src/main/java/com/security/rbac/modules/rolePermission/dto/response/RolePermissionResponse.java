package com.security.rbac.modules.rolePermission.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Response returned after assigning role permissions.
 *
 * @param roleId      the role that was updated
 * @param saved       count of newly created permissions
 * @param skipped     count of duplicates that already existed (skipped)
 * @param permissions list of all active permissions for the role after the
 *                    operation
 */
@JsonPropertyOrder({
        "roleId",
        "saved",
        "skipped",
        "permissions"
})
public record RolePermissionResponse(
        Long roleId,
        int saved,
        int skipped,
        List<PermissionDetail> permissions) {
    public record PermissionDetail(
            Long id,
            Long moduleId,
            String moduleName,
            Long actionId,
            String actionName,
            Boolean allowed) {
    }
}
