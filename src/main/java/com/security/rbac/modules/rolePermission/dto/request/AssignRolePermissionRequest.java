package com.security.rbac.modules.rolePermission.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request body for assigning permissions to a role.
 *
 * <pre>
 * {
 *   "roleId": 1,
 *   "permissions": [
 *     { "moduleId": 1, "actionId": 1 },
 *     { "moduleId": 1, "actionId": 2 }
 *   ]
 * }
 * </pre>
 */
public record AssignRolePermissionRequest(

        @NotNull(message = "roleId is required") Long roleId,

        @NotEmpty(message = "permissions must not be empty") @Valid List<PermissionEntry> permissions

) {
    /** A single module + action pair inside the permissions list. */
    public record PermissionEntry(

            @NotNull(message = "moduleId is required") Long moduleId,

            @NotNull(message = "actionId is required") Long actionId) {
    }
}
