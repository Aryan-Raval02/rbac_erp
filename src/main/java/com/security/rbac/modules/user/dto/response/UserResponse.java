package com.security.rbac.modules.user.dto.response;

import java.time.Instant;

/**
 * Response returned after creating or fetching a tenant user.
 * Password is intentionally excluded.
 */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        String username,
        String phoneNumber,
        Long roleId,
        String roleName,
        Boolean isActive,
        Instant createdAt) {
}
