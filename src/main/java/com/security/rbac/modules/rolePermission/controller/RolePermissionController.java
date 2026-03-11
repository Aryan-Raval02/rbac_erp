package com.security.rbac.modules.rolePermission.controller;

import com.security.rbac.modules.rolePermission.dto.request.AssignRolePermissionRequest;
import com.security.rbac.modules.rolePermission.dto.response.RolePermissionResponse;
import com.security.rbac.modules.rolePermission.service.RolePermissionServiceImpl;
import com.security.rbac.utility.ResponseBuilder;
import com.security.rbac.utility.ResponseStructure;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing role permissions.
 *
 * <p>
 * Base path: {@code /api/v1/role-permissions}
 */
@RestController
@RequestMapping("/api/v1/role-permissions")
public class RolePermissionController {

    private final RolePermissionServiceImpl rolePermissionService;

    public RolePermissionController(RolePermissionServiceImpl rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    /**
     * Assign permissions to a role.
     *
     * <p>
     * Request:
     * 
     * <pre>
     * POST /api/v1/role-permissions
     * {
     *   "roleId": 1,
     *   "permissions": [
     *     { "moduleId": 1, "actionId": 1 },
     *     { "moduleId": 1, "actionId": 2 }
     *   ]
     * }
     * </pre>
     *
     * <p>
     * Response: {@code 201 Created} with save/skip summary and full permission
     * list.
     * <p>
     * Duplicate entries are silently skipped (idempotent).
     */
    @PostMapping
    @PreAuthorize("hasRole('SERAVION')")
    public ResponseEntity<ResponseStructure<RolePermissionResponse>> assignPermissions(
            @Valid @RequestBody AssignRolePermissionRequest request) {

        RolePermissionResponse response = rolePermissionService.assignPermissions(request);
        return ResponseBuilder.success(HttpStatus.OK, "Role Permission Created !!", response);
    }

    /**
     * Get all current permissions for a role.
     *
     * <pre>
     * GET /api/v1/role-permissions/{roleId}
     * </pre>
     *
     * <p>
     * Response: {@code 200 OK} with full permission list.
     */
    @GetMapping
    public ResponseEntity<ResponseStructure<RolePermissionResponse>> getPermissions(@RequestParam Long roleId) {
        return ResponseBuilder.success(HttpStatus.OK, "Role Permission Fetched !!", rolePermissionService.getPermissions(roleId));
    }
}
