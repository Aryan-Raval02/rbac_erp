package com.security.rbac.modules.tenant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a registered tenant in the shared {@code public.tenant_registry}
 * table.
 *
 * <p>
 * This entity is always accessed in the {@code public} schema regardless of the
 * active tenant context. Services that interact with this entity should set
 * {@code TenantContext.setCurrentTenant(TenantContext.DEFAULT_TENANT)} before
 * any
 * operation and restore the original value afterwards.
 */
@Entity
@Table(name = "tenant_registry", schema = "public", // always public schema
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_tenant_schema", columnNames = "schema_name")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable name (e.g. "Acme Corp"). */
    @Column(name = "tenant_name", nullable = false, length = 100)
    private String tenantName;

    /**
     * PostgreSQL schema name used for this tenant's isolated data.
     * Derived from {@code tenantName}: lowercased, spaces → underscores.
     */
    @Column(name = "schema_name", nullable = false, unique = true, length = 100)
    private String schemaName;

    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * Lifecycle status of the tenant.
     * Possible values: ACTIVE, SUSPENDED, DELETED.
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
