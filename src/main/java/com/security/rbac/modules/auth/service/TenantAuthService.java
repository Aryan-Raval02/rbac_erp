package com.security.rbac.modules.auth.service;

import com.security.rbac.jwt.JwtProperties;
import com.security.rbac.jwt.JwtService;
import com.security.rbac.modules.auth.dto.request.LoginRequest;
import com.security.rbac.modules.auth.dto.response.AuthResponse;
import com.security.rbac.modules.permissionQuery.service.PermissionQueryService;
import com.security.rbac.modules.user.entity.User;
import com.security.rbac.modules.user.repo.UserRepository;
import com.security.rbac.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import com.security.rbac.exception.InactiveUserException;
import com.security.rbac.exception.InvalidCredentialsException;
import com.security.rbac.exception.TenantNotProvidedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TenantAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PermissionQueryService permissionQueryService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String currentTenant = TenantContext.getCurrentTenant();
        if (TenantContext.DEFAULT_TENANT.equals(currentTenant)) {
            throw new TenantNotProvidedException("Tenant context is missing or invalid");
        }

        // 1. Search for user by username or email in the tenant schema
        User tenantUser = userRepository.findByUsernameOrEmail(request.identifier(), request.identifier())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        // 2. Verify active status
        if (!Boolean.TRUE.equals(tenantUser.getIsActive())) {
            throw new InactiveUserException("Account is disabled");
        }

        // 3. Verify password
        if (!passwordEncoder.matches(request.password(), tenantUser.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // 4. Update last login time
        tenantUser.setLastLoginAt(Instant.now());
        userRepository.save(tenantUser);

        // 5. Build JWT Claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", "TENANT");
        claims.put("userId", tenantUser.getId());
        claims.put("roleId", tenantUser.getRole().getId());
        claims.put("role", tenantUser.getRole().getName());
        claims.put("tenantSchema", currentTenant); // Include the schema so the JWT filter knows where to route

        // 6. Generate tokens
        String accessToken = jwtService.generateAccessToken(claims, tenantUser.getUsername());
        String refreshToken = jwtService.generateRefreshToken(claims, tenantUser.getUsername());

        // 7. Load permissions for frontend
        List<AuthResponse.PermissionModuleDto> permissions =
                permissionQueryService.getGroupedPermissions(
                        tenantUser.getId(),
                        tenantUser.getUsername(),
                        currentTenant
                );

        // 8. Return response
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                "TENANT",
                jwtProperties.expirationMs(),
                tenantUser.getUsername(),
                tenantUser.getId(),
                tenantUser.getRole().getName(),
                currentTenant,
                permissions
        );
    }
}
