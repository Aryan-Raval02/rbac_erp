package com.security.rbac.modules.auth.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        String userType,
        long expiresIn,

        // Frontend-friendly decoded fields
        String username,
        Long userId,
        String role,
        String tenantSchema,

        List<PermissionModuleDto> permissions
) {
    public record PermissionModuleDto(
            Long moduleId,
            String moduleName,
            String label,
            List<ActionDto> actions
    ) {
    }

    public record ActionDto(
            Long actionId,
            String actionName,
            String label
    ) {
    }
}
