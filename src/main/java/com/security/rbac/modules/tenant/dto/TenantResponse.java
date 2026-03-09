package com.security.rbac.modules.tenant.dto;

import java.time.Instant;

/**
 * Response payload after successful tenant creation.
 *
 * @param id         Generated ID from {@code public.tenant_registry}
 * @param tenantName Original tenant name
 * @param schemaName Derived PostgreSQL schema name
 * @param status     Lifecycle status (should be "ACTIVE")
 * @param createdAt  Creation timestamp (UTC)
 */
public record TenantResponse(
        Long id,
        String tenantName,
        String schemaName,
        String displayName,
        String status,
        Instant createdAt) {
}
