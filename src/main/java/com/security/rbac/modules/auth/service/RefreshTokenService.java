package com.security.rbac.modules.auth.service;

import com.security.rbac.jwt.exception.InvalidTokenException;
import com.security.rbac.jwt.JwtService;
import com.security.rbac.modules.auth.dto.response.AuthResponse;
import com.security.rbac.modules.ceo.entity.GlobalUser;
import com.security.rbac.modules.ceo.repo.GlobalUserRepository;
import com.security.rbac.modules.permissionQuery.service.PermissionQueryService;
import com.security.rbac.modules.root.entity.RootUser;
import com.security.rbac.modules.root.repo.RootUserRepository;
import com.security.rbac.modules.user.entity.User;
import com.security.rbac.modules.user.repo.UserRepository;
import com.security.rbac.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtService jwtService;
    private final RootUserRepository rootUserRepository;
    private final GlobalUserRepository globalUserRepository;
    private final UserRepository userRepository;
    private final PermissionQueryService permissionQueryService;

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        // 1. Validate token
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        // 2. Extract minimal claims
        String username = jwtService.extractUsername(refreshToken);
        String userType = jwtService.extractUserType(refreshToken);
        Long userId = jwtService.extractClaim(refreshToken, claims -> claims.get("userId", Long.class));
        String tenantSchema = jwtService.extractTenantSchema(refreshToken);

        if (username == null || userType == null || userId == null) {
            throw new InvalidTokenException("Invalid refresh token payload");
        }

        // 3. Process based on user type
        return switch (userType.toUpperCase()) {
            case "ROOT" -> processRootRefresh(userId, username, refreshToken);
            case "CEO" -> processCeoRefresh(userId, username, tenantSchema, refreshToken);
            case "TENANT" -> processTenantRefresh(userId, username, tenantSchema, refreshToken);
            default -> throw new InvalidTokenException("Unknown user type in refresh token");
        };
    }

    private AuthResponse processRootRefresh(Long userId, String username, String existingRefreshToken) {
        RootUser rootUser = rootUserRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User no longer exists"));

        if (!Boolean.TRUE.equals(rootUser.getActive()) || !rootUser.getUsername().equals(username)) {
            throw new InvalidTokenException("User is inactive or token is invalid");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", "ROOT");
        claims.put("userId", rootUser.getId());
        claims.put("role", rootUser.getRole().name());

        return buildAuthResponse(
                jwtService.generateAccessToken(claims, rootUser.getUsername()),
                existingRefreshToken,
                "ROOT",
                rootUser.getUsername(),
                rootUser.getId(),
                rootUser.getRole().name(),
                "public",
                null
        );
    }

    private AuthResponse processCeoRefresh(Long userId, String username, String tenantSchema, String existingRefreshToken) {
        GlobalUser ceoUser = globalUserRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User no longer exists"));

        if (!Boolean.TRUE.equals(ceoUser.getIsActive()) || !ceoUser.getUsername().equals(username)) {
            throw new InvalidTokenException("User is inactive or token is invalid");
        }

        boolean hasSchema = Boolean.TRUE.equals(ceoUser.getHasSchema());
        String tenant = hasSchema ? ceoUser.getTargetSchema() : null;

        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", "CEO");
        claims.put("userId", ceoUser.getId());
        claims.put("role", ceoUser.getSystemRole());          // always CEO
        claims.put("tenantSchema", tenant); // tenant owned by CEO
        claims.put("hasSchema", hasSchema);

        ceoUser.setLastLoginAt(Instant.now());
        globalUserRepository.save(ceoUser);

        List<AuthResponse.PermissionModuleDto> permissions = null;
        if (hasSchema && tenantSchema != null && !tenantSchema.isBlank()) {
            permissions = permissionQueryService.getGroupedPermissions(
                    ceoUser.getId(),
                    ceoUser.getUsername(),
                    tenantSchema
            );
        }

        return buildAuthResponse(
                jwtService.generateAccessToken(claims, ceoUser.getUsername()),
                existingRefreshToken,
                "CEO",
                ceoUser.getUsername(),
                ceoUser.getId(),
                ceoUser.getSystemRole(),
                tenant,
                permissions
        );
    }

    private AuthResponse processTenantRefresh(Long userId, String username, String tenantSchema, String existingRefreshToken) {
        if (tenantSchema == null) {
            throw new InvalidTokenException("Tenant schema missing in refresh token");
        }

        // Important: Set tenant context BEFORE querying tenant repository
        try {
            TenantContext.setCurrentTenant(tenantSchema);

            User tenantUser = userRepository.findById(userId)
                    .orElseThrow(() -> new InvalidTokenException("User no longer exists"));

            if (!Boolean.TRUE.equals(tenantUser.getIsActive()) || !tenantUser.getUsername().equals(username)) {
                throw new InvalidTokenException("User is inactive or token is invalid");
            }

            Map<String, Object> claims = new HashMap<>();
            claims.put("userType", "TENANT");
            claims.put("userId", tenantUser.getId());
            claims.put("roleId", tenantUser.getRole().getId());
            claims.put("role", tenantUser.getRole().getName());
            claims.put("tenantSchema", tenantSchema);

            tenantUser.setLastLoginAt(Instant.now());
            userRepository.save(tenantUser);

            List<AuthResponse.PermissionModuleDto> permissions =
                    permissionQueryService.getGroupedPermissions(
                            tenantUser.getId(),
                            tenantUser.getUsername(),
                            tenantSchema
                    );

            return buildAuthResponse(
                    jwtService.generateAccessToken(claims, tenantUser.getUsername()),
                    existingRefreshToken,
                    "TENANT",
                    tenantUser.getUsername(),
                    tenantUser.getId(),
                    tenantUser.getRole().getName(),
                    tenantSchema,
                    permissions
            );
        } finally {
            TenantContext.clear();
        }
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, String userType,
                                           String username, Long userId, String role, String tenantSchema, List<AuthResponse.PermissionModuleDto> permissions) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userType(userType)
                .expiresIn(jwtService.extractExpiration(accessToken).getTime())
                .username(username)
                .userId(userId)
                .role(role)
                .tenantSchema(tenantSchema)
                .build();
    }
}
