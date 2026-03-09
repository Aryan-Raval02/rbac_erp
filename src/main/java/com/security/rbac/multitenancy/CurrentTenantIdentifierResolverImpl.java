package com.security.rbac.multitenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Resolves the current tenant identifier for Hibernate.
 *
 * <p>
 * Hibernate calls {@link #resolveCurrentTenantIdentifier()} before every
 * database interaction to determine which schema to use.
 *
 * <p>
 * <b>Pitfall:</b> If the thread-local is empty (e.g. during startup,
 * health-checks, or Flyway baseline), we return
 * {@value TenantContext#DEFAULT_TENANT}
 * so that Hibernate targets the {@code public} schema instead of throwing.
 */
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantContext.getCurrentTenant();
    }

    /**
     * When {@code true}, Hibernate will validate that the schema returned by
     * {@link #resolveCurrentTenantIdentifier()} is the same across a Session's
     * lifetime.
     * Return {@code false} to allow schema changes within a request (rare but
     * safe).
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
