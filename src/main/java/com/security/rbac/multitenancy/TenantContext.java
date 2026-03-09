package com.security.rbac.multitenancy;

/**
 * Holds the current tenant identifier (schema name) for the executing thread.
 *
 * <p>
 * Usage pattern:
 * 
 * <pre>
 * TenantContext.setCurrentTenant("acme_corp");
 * try {
 *     // ... tenant-scoped DB operations
 * } finally {
 *     TenantContext.clear();
 * }
 * </pre>
 *
 * <p>
 * <b>Pitfall:</b> Always call {@link #clear()} in a {@code finally} block or
 * via a
 * {@code Filter}/{@code Interceptor} to prevent thread-local leaks in
 * thread-pool
 * environments (e.g., Tomcat, HikariCP worker threads).
 */
public final class TenantContext {

    /**
     * Fallback schema used when no tenant is set (e.g. public-schema operations).
     */
    public static final String DEFAULT_TENANT = "public";

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        /* utility class – no instances */ }

    /**
     * Sets the tenant schema name for the current thread.
     *
     * @param tenantId the PostgreSQL schema name (e.g. "acme_corp")
     */
    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Returns the current tenant schema name, or {@value #DEFAULT_TENANT} if none
     * is set.
     */
    public static String getCurrentTenant() {
        String tenant = CURRENT_TENANT.get();
        return (tenant != null && !tenant.isBlank()) ? tenant : DEFAULT_TENANT;
    }

    /**
     * Clears the current tenant from the thread-local.
     * <b>Must</b> be called after each request/unit-of-work.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
