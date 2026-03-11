package com.security.rbac.modules.permissionQuery.service;

import com.security.rbac.modules.auth.dto.response.AuthResponse;

import java.util.List;

public interface PermissionQueryService {
    List<AuthResponse.PermissionModuleDto> getGroupedPermissions(
            Long globalUserId,
            String username,
            String tenantSchema
    );
}