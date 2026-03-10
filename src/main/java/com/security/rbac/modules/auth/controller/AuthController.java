package com.security.rbac.modules.auth.controller;

import com.security.rbac.modules.auth.dto.request.LoginRequest;
import com.security.rbac.modules.auth.dto.request.RefreshTokenRequest;
import com.security.rbac.modules.auth.dto.response.AuthResponse;
import com.security.rbac.modules.auth.service.CeoAuthService;
import com.security.rbac.modules.auth.service.RefreshTokenService;
import com.security.rbac.modules.auth.service.RootAuthService;
import com.security.rbac.modules.auth.service.TenantAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for all user login types")
public class AuthController {

    private final RootAuthService rootAuthService;
    private final CeoAuthService ceoAuthService;
    private final TenantAuthService tenantAuthService;
    private final RefreshTokenService refreshTokenService;

    @Operation(summary = "Login for Root/Platform Admins")
    @PostMapping("/root/login")
    public ResponseEntity<AuthResponse> rootLogin(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = rootAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Login for Tenant CEOs/Owners")
    @PostMapping("/ceo/login")
    public ResponseEntity<AuthResponse> ceoLogin(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = ceoAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Login for Tenant Users (requires X-Tenant-ID header)")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> tenantLogin(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantId,
            @Valid @RequestBody LoginRequest request) {

        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "X-Tenant-ID header is missing or empty");
        }

        AuthResponse response = tenantAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refresh access token for any user type")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = refreshTokenService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('BRANCH_MANAGEMENT_READ') or hasRole('CEO')")
    @GetMapping("/check/permission/branch")
    public ResponseEntity<String> branchReadAccess(){
        return ResponseEntity.ok("Accessed");
    }

    @PreAuthorize("hasAuthority('PRODUCT_MANAGEMENT_READ') or hasRole('CEO')")
    @GetMapping("/check/permission/product")
    public ResponseEntity<String> productReadAccess(){
        return ResponseEntity.ok("Accessed");
    }

    @GetMapping("/me")
    public Object me(Authentication authentication) {
        return Map.of(
                "name", authentication.getName(),
                "authorities", authentication.getAuthorities()
        );
    }

}
