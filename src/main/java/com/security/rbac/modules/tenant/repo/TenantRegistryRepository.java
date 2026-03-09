package com.security.rbac.modules.tenant.repo;

import com.security.rbac.modules.tenant.entity.TenantRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for the {@code public.tenant_registry} table.
 *
 * <p>
 * <b>Important:</b> Since {@link TenantRegistry} is mapped with
 * {@code schema = "public"}, Hibernate will always qualify queries with
 * {@code public.tenant_registry}. You do NOT need to manipulate
 * {@link com.security.rbac.multitenancy.TenantContext} when using this
 * repository — it is schema-safe by design.
 */
@Repository
public interface TenantRegistryRepository extends JpaRepository<TenantRegistry, Long> {

    Optional<TenantRegistry> findBySchemaName(String schemaName);

    boolean existsBySchemaName(String schemaName);

    Optional<TenantRegistry> findByTenantName(String tenantName);
}
