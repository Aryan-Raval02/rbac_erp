package com.security.rbac.modules.rolePermission.service;

import com.security.rbac.modules.rolePermission.dto.request.AssignRolePermissionRequest;
import com.security.rbac.modules.rolePermission.dto.response.RolePermissionResponse;
import org.springframework.transaction.annotation.Transactional;

public interface RolePermissionService {
    @Transactional
    RolePermissionResponse assignPermissions(AssignRolePermissionRequest request);

    @Transactional(readOnly = true)
    RolePermissionResponse getPermissions(Long roleId);
}
