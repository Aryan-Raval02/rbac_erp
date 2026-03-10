package com.security.rbac.modules.auth.service;

import com.security.rbac.exception.InvalidTokenException;
import com.security.rbac.jwt.JwtService;
import com.security.rbac.modules.auth.dto.response.AuthResponse;
import com.security.rbac.modules.ceo.entity.GlobalUser;
import com.security.rbac.modules.ceo.repo.GlobalUserRepository;
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
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtService jwtService;
    private final RootUserRepository rootUserRepository;
    private final GlobalUserRepository globalUserRepository;
    private final UserRepository userRepository;

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
            case "ROOT" -> processRootRefresh(userId, username);
            case "CEO" -> processCeoRefresh(userId, username, tenantSchema);
            case "TENANT" -> processTenantRefresh(userId, username, tenantSchema);
            default -> throw new InvalidTokenException("Unknown user type in refresh token");
        };
    }

    private AuthResponse processRootRefresh(Long userId, String username) {
        RootUser rootUser = rootUserRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User no longer exists"));

        if (!Boolean.TRUE.equals(rootUser.getActive()) || !rootUser.getUsername().equals(username)) {
            throw new InvalidTokenException("User is inactive or token is invalid");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", "ROOT");
        claims.put("userId", rootUser.getId());
        claims.put("role", rootUser.getRole().name());

        Map<String, Object> refreshClaims = new HashMap<>();
        refreshClaims.put("userType", "ROOT");
        refreshClaims.put("userId", rootUser.getId());

        return buildAuthResponse(
                jwtService.generateAccessToken(claims, rootUser.getUsername()),
                jwtService.generateRefreshToken(refreshClaims, rootUser.getUsername()),
                "ROOT",
                rootUser.getUsername(),
                rootUser.getId(),
                rootUser.getRole().name(),
                null);
    }

    private AuthResponse processCeoRefresh(Long userId, String username, String tenantSchema) {
        GlobalUser ceoUser = globalUserRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User no longer exists"));

        if (!Boolean.TRUE.equals(ceoUser.getIsActive()) || !ceoUser.getUsername().equals(username)) {
            throw new InvalidTokenException("User is inactive or token is invalid");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", "CEO");
        claims.put("userId", ceoUser.getId());
        claims.put("role", "CEO");
        claims.put("tenantSchema", ceoUser.getTargetSchema());

        Map<String, Object> refreshClaims = new HashMap<>();
        refreshClaims.put("userType", "CEO");
        refreshClaims.put("userId", ceoUser.getId());
        refreshClaims.put("tenantSchema", ceoUser.getTargetSchema());

        ceoUser.setLastLoginAt(Instant.now());
        globalUserRepository.save(ceoUser);

        return buildAuthResponse(
                jwtService.generateAccessToken(claims, ceoUser.getUsername()),
                jwtService.generateRefreshToken(refreshClaims, ceoUser.getUsername()),
                "CEO",
                ceoUser.getUsername(),
                ceoUser.getId(),
                ceoUser.getSystemRole(),
                ceoUser.getTargetSchema());
    }

    private AuthResponse processTenantRefresh(Long userId, String username, String tenantSchema) {
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
            claims.put("role", tenantUser.getRole().getName());
            claims.put("tenantSchema", tenantSchema);

            Map<String, Object> refreshClaims = new HashMap<>();
            refreshClaims.put("userType", "TENANT");
            refreshClaims.put("userId", tenantUser.getId());
            refreshClaims.put("tenantSchema", tenantSchema);

            tenantUser.setLastLoginAt(Instant.now());
            userRepository.save(tenantUser);

            return buildAuthResponse(
                    jwtService.generateAccessToken(claims, tenantUser.getUsername()),
                    jwtService.generateRefreshToken(refreshClaims, tenantUser.getUsername()),
                    "TENANT",
                    tenantUser.getUsername(),
                    tenantUser.getId(),
                    tenantUser.getRole().getName(),
                    tenantSchema);
        } finally {
            TenantContext.clear();
        }
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, String userType,
            String username, Long userId, String role, String tenantSchema) {
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
