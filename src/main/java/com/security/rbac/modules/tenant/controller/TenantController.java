package com.security.rbac.modules.tenant.controller;

import com.security.rbac.modules.tenant.dto.CreateTenantRequest;
import com.security.rbac.modules.tenant.dto.TenantResponse;
import com.security.rbac.modules.tenant.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for tenant provisioning.
 *
 * <p>
 * Endpoint: {@code POST /api/v1/tenants}
 *
 * <p>
 * <b>Access control:</b> This endpoint should be restricted to
 * {@code SUPER_ADMIN} or platform operators in production.
 * The {@link com.security.rbac.config.SecurityConfig} whitelists
 * this path only during development.
 */
@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Provisions a new tenant: creates the PostgreSQL schema, runs Flyway
     * migrations, and registers the tenant in {@code public.tenant_registry}.
     *
     * @param request tenant creation payload
     * @return {@code 201 CREATED} with the provisioned {@link TenantResponse}
     */
    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(
            @Valid @RequestBody CreateTenantRequest request) {

        TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
