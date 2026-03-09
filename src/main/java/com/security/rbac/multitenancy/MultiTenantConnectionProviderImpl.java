package com.security.rbac.multitenancy;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Provides JDBC connections scoped to a tenant schema by executing
 * {@code SET search_path TO <schema>, public} on every connection checkout.
 *
 * <p>
 * <b>Key design decisions:</b>
 * <ul>
 * <li>Connections are still pooled by HikariCP; we only switch the
 * {@code search_path} at borrow time.</li>
 * <li>On release we reset to {@code public} to avoid leaking tenant context
 * back into the pool.</li>
 * <li>The fallback {@code , public} in search_path ensures shared-schema
 * objects (e.g. extensions) remain visible.</li>
 * </ul>
 *
 * <p>
 * <b>Pitfall:</b> {@code auto-commit} must be {@code false} in HikariCP
 * ({@code spring.datasource.hikari.auto-commit=false}) and
 * {@code hibernate.connection.provider_disables_autocommit=true} set, otherwise
 * Hibernate may commit the schema change before the query runs.
 */
public class MultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;

    public MultiTenantConnectionProviderImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ── Borrow / Release ──────────────────────────────────────────────────────

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    /**
     * Called by Hibernate when it needs a connection for {@code tenantIdentifier}.
     * We set the PostgreSQL search_path so all un-qualified queries target the
     * tenant schema first, then fall through to public.
     */
    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        final Connection connection = dataSource.getConnection();
        setSearchPath(connection, tenantIdentifier);
        return connection;
    }

    /**
     * Called by Hibernate when releasing the connection.
     * We reset search_path to public to keep the pool clean.
     */
    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        resetSearchPath(connection);
        connection.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setSearchPath(Connection connection, String schema) throws SQLException {
        // Sanitize schema name to prevent SQL injection
        String safeSchema = sanitizeSchemaName(schema);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO \"" + safeSchema + "\", public");
        }
    }

    private void resetSearchPath(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO public");
        }
    }

    /**
     * Basic guard: schema names must be alphanumeric + underscores only.
     * Postgres identifiers are already case-folded to lowercase.
     */
    private String sanitizeSchemaName(String schema) {
        if (schema == null || schema.isBlank()) {
            throw new IllegalArgumentException("Schema name must not be blank.");
        }
        // Lowercase FIRST — then validate. Never allow uppercase through.
        String lower = schema.toLowerCase().trim();
        if (!lower.matches("[a-z][a-z0-9_]{0,62}")) {
            throw new IllegalArgumentException(
                    "Invalid schema name '" + schema + "'. " +
                            "Must start with a letter and contain only lowercase letters, digits, underscores (max 63 chars).");
        }
        return lower;
    }

    // ── Contract ─────────────────────────────────────────────────────────────

    @Override
    public boolean supportsAggressiveRelease() {
        // Return false: we are NOT using JTA, so Hibernate should not aggressively
        // release connections between transactions.
        return false;
    }

    @Override
    public boolean isUnwrappableAs(@SuppressWarnings("rawtypes") Class unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        throw new UnsupportedOperationException("Unwrap not supported");
    }
}
