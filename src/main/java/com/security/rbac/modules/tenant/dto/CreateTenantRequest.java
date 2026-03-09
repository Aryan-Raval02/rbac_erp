package com.security.rbac.modules.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new tenant.
 *
 * @param tenantName  Human-readable name (e.g. "Acme Corp").
 * @param displayName Optional marketing display name.
 */
public record CreateTenantRequest(

        @NotBlank(message = "Tenant name is required")
        @Size(min = 2, max = 100, message = "Tenant name must be 2–100 characters")
        String tenantName,

        @Size(max = 255)
        String displayName
) { }
