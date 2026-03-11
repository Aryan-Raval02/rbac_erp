package com.security.rbac.modules.auth.service;

import com.security.rbac.jwt.JwtProperties;
import com.security.rbac.jwt.JwtService;
import com.security.rbac.modules.auth.dto.request.LoginRequest;
import com.security.rbac.modules.auth.dto.response.AuthResponse;
import com.security.rbac.modules.ceo.entity.GlobalUser;
import com.security.rbac.modules.ceo.repo.GlobalUserRepository;
import com.security.rbac.jwt.exception.InactiveUserException;
import com.security.rbac.jwt.exception.InvalidCredentialsException;
import com.security.rbac.modules.permissionQuery.service.PermissionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CeoAuthService {

    private final GlobalUserRepository globalUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PermissionQueryService permissionQueryService;

    public AuthResponse login(LoginRequest request) {
        // 1. Search for user by username or email
        GlobalUser ceoUser = globalUserRepository
                .findByUsernameOrEmail(request.identifier(), request.identifier())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        // 2. Verify active status
        if (!Boolean.TRUE.equals(ceoUser.getIsActive())) {
            throw new InactiveUserException("Account is disabled");
        }

        // 3. Verify password
        if (!passwordEncoder.matches(request.password(), ceoUser.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // 4. Update last login time
        ceoUser.setLastLoginAt(Instant.now());
        globalUserRepository.save(ceoUser);

        boolean hasSchema = Boolean.TRUE.equals(ceoUser.getHasSchema());
        String tenantSchema = hasSchema ? ceoUser.getTargetSchema() : null;

        // 5. Build JWT claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", "CEO");
        claims.put("userId", ceoUser.getId());
        claims.put("role", ceoUser.getSystemRole());          // always CEO
        claims.put("tenantSchema", tenantSchema); // tenant owned by CEO
        claims.put("hasSchema", hasSchema);

        // 6. Generate tokens
        String accessToken = jwtService.generateAccessToken(claims, ceoUser.getUsername());
        String refreshToken = jwtService.generateRefreshToken(claims, ceoUser.getUsername());

        // 7. Conditionally load permissions
        List<AuthResponse.PermissionModuleDto> permissions = null;
        if (hasSchema && tenantSchema != null && !tenantSchema.isBlank()) {
            permissions = permissionQueryService.getGroupedPermissions(
                    ceoUser.getId(),
                    ceoUser.getUsername(),
                    tenantSchema
            );
        }

        // 8. Return response
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                "CEO",
                jwtProperties.expirationMs(),
                ceoUser.getUsername(),
                ceoUser.getId(),
                ceoUser.getSystemRole(),
                ceoUser.getTargetSchema(),
                permissions
        );
    }
}
