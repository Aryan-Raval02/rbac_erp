package com.security.rbac.modules.auth.dto.response;

import lombok.Builder;

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
        String tenantSchema) {
}
