package com.security.rbac.modules.auth.service;

import com.security.rbac.jwt.JwtProperties;
import com.security.rbac.jwt.JwtService;
import com.security.rbac.modules.auth.dto.request.LoginRequest;
import com.security.rbac.modules.auth.dto.response.AuthResponse;
import com.security.rbac.modules.root.entity.RootUser;
import com.security.rbac.modules.root.repo.RootUserRepository;
import com.security.rbac.jwt.exception.InactiveUserException;
import com.security.rbac.jwt.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RootAuthService {

    private final RootUserRepository rootUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 1. Search for user by username or email
        RootUser rootUser = rootUserRepository.findByUsernameOrEmail(request.identifier(), request.identifier())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        // 2. Verify active status
        if (!Boolean.TRUE.equals(rootUser.getActive())) {
            throw new InactiveUserException("Account is disabled");
        }

        // 3. Verify password
        if (!passwordEncoder.matches(request.password(), rootUser.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // 4. Build JWT Claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", "ROOT");
        claims.put("userId", rootUser.getId());
        claims.put("role", rootUser.getRole().name()); // Assuming Roles enum
        // Note: No tenantSchema required for ROOT

        // 5. Generate tokens
        String accessToken = jwtService.generateAccessToken(claims, rootUser.getUsername());
        String refreshToken = jwtService.generateRefreshToken(claims, rootUser.getUsername());

        // 6. Return response
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                "ROOT",
                jwtProperties.expirationMs(),
                rootUser.getUsername(),
                rootUser.getId(),
                rootUser.getRole().name(),
                "public",
                null
        );
    }
}
