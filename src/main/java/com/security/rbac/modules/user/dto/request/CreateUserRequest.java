package com.security.rbac.modules.user.dto.request;

import com.security.rbac.modules.rolePermission.dto.request.AssignRolePermissionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for creating a new tenant user.
 *
 * <pre>
 * POST /api/v1/users
 * {
 *   "fullName"    : "Jane Smith",
 *   "email"       : "jane@acme.com",
 *   "username"    : "jane_smith",
 *   "password"    : "Jane@2024!",
 *   "phoneNumber" : "9876543210",
 *   "roleId"      : 2
 * }
 * </pre>
 */
public record CreateUserRequest(

        @NotBlank(message = "fullName is required") String fullName,

        @NotBlank(message = "email is required") @Email(message = "email must be a valid email address") String email,

        @NotBlank(message = "username is required") @Size(min = 3, max = 50, message = "username must be 3–50 characters") String username,

        @NotBlank(message = "password is required") @Size(min = 8, message = "password must be at least 8 characters") String password,

        String phoneNumber, // optional

        @NotNull(message = "roleId is required") Long roleId,

        @Valid List<PermissionRequest> permissions

) {
    public record PermissionRequest(

            @NotNull(message = "moduleId is required")
            Long moduleId,

            @NotNull(message = "actionId is required")
            Long actionId

    ) {
    }
}
