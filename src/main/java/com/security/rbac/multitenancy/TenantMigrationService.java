package com.security.rbac.multitenancy;

import com.security.rbac.modules.ceo.entity.GlobalUser;
import com.security.rbac.modules.ceo.repo.GlobalUserRepository;
import com.security.rbac.modules.role.entity.Role;
import com.security.rbac.modules.role.exception.RoleNotFoundException;
import com.security.rbac.modules.role.repo.RoleRepository;
import com.security.rbac.modules.rolePermission.entity.RolePermission;
import com.security.rbac.modules.rolePermission.repo.RolePermissionRepository;
import com.security.rbac.modules.user.entity.User;
import com.security.rbac.modules.user.repo.UserRepository;
import com.security.rbac.modules.userPermission.entity.UserPermission;
import com.security.rbac.modules.userPermission.repo.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Runs Flyway migrations programmatically for a specific tenant schema.
 *
 * <p>
 * Called once during tenant onboarding (see
 * {@code TenantService.createTenant}).
 * It is <em>never</em> invoked at application startup for all tenants.
 *
 * <h2>Concurrency safety</h2>
 * <p>
 * A per-tenant {@link ReentrantLock} prevents two threads from migrating the
 * same schema simultaneously (e.g., two concurrent signup requests for the same
 * tenant name). The lock map is in-process only; for distributed deployments
 * add
 * a distributed lock (Redis SETNX, database advisory lock, etc.).
 *
 * <h2>Flyway history table location</h2>
 * <p>
 * Each tenant schema gets its own {@code flyway_schema_history} table. This
 * keeps tenant migration state isolated and avoids locking contention on a
 * shared table.
 *
 * <h2>Pitfalls addressed</h2>
 * <ul>
 * <li>{@code baselineOnMigrate=true} – required when the schema already exists
 * (e.g. created by {@code CREATE SCHEMA}), otherwise Flyway refuses to
 * run.</li>
 * <li>{@code validateOnMigrate=true} – catches checksum drift from edited
 * scripts.</li>
 * <li>Schema name sanitization – prevents SQL injection via schema name.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TenantMigrationService {

    private static final Logger log = LoggerFactory.getLogger(TenantMigrationService.class);

    /** Per-tenant lock map to prevent concurrent migration of the same schema. */
    private final ConcurrentHashMap<String, ReentrantLock> tenantLocks = new ConcurrentHashMap<>();

    private final DataSource dataSource;
    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final GlobalUserRepository globalUserRepository;
    private final RoleRepository roleRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates the PostgreSQL schema (if it does not already exist) and runs
     * Flyway tenant migrations for that schema.
     *
     * @param tenantSchema the schema / tenant name (alphanumeric + underscores)
     * @throws IllegalArgumentException if the schema name is invalid
     * @throws TenantMigrationException if Flyway migration fails
     */
    public void migrateTenant(String tenantSchema) {
        String safeSchema = sanitize(tenantSchema);
        ReentrantLock lock = tenantLocks.computeIfAbsent(safeSchema, k -> new ReentrantLock());

        lock.lock();
        try {
            createSchemaIfNotExists(safeSchema);
            runFlywayMigration(safeSchema);
            log.info("Tenant schema '{}' migrated successfully.", safeSchema);
        } finally {
            lock.unlock();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Executes {@code CREATE SCHEMA IF NOT EXISTS} directly on the datasource
     * (bypasses Hibernate search_path to avoid chicken-egg).
     *
     * <p>
     * <b>CRITICAL:</b> HikariCP is configured with {@code auto-commit=false}.
     * PostgreSQL DDL is transactional, so without an explicit commit the schema
     * creation is silently rolled back when the connection is returned to the pool.
     * We temporarily enable auto-commit for this DDL-only connection, then restore
     * it.
     */
    private void createSchemaIfNotExists(String schema) {
        // Unquoted identifier → PostgreSQL folds to lowercase automatically.
        // Quoted identifiers ("Schema") are case-sensitive and can create duplicates.
        String sql = "CREATE SCHEMA IF NOT EXISTS " + schema;
        try (Connection conn = dataSource.getConnection()) {
            // Temporarily enable auto-commit so DDL is not rolled back
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(true);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                log.debug("Schema '{}' created (or already existed).", schema);
            } finally {
                conn.setAutoCommit(originalAutoCommit); // always restore
            }
        } catch (SQLException e) {
            throw new TenantMigrationException(
                    "Failed to create schema '" + schema + "'", e);
        }
    }

    /**
     * Configures a fresh Flyway instance scoped to the tenant schema and runs
     * pending migrations.
     *
     * <p>
     * Key configuration:
     * <ul>
     * <li>{@code schemas + defaultSchema} = tenant schema</li>
     * <li>{@code locations} = tenant-specific migration scripts only</li>
     * <li>{@code table} = history table lives inside the tenant schema</li>
     * <li>{@code baselineOnMigrate=true} = handles already-existing schema</li>
     * </ul>
     */
    private void runFlywayMigration(String schema) {
        try {
            // Always lowercase — Flyway forwards the value to the JDBC driver which
            // may quote it before sending to PostgreSQL, causing a new schema to be
            // created.
            String lcSchema = schema.toLowerCase();
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(lcSchema)
                    .defaultSchema(lcSchema)
                    .locations("classpath:db/migration/tenant")
                    .table("flyway_schema_history") // per-tenant history table
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .validateOnMigrate(true)
                    .outOfOrder(false)
                    .load();
            flyway.migrate();
        } catch (Exception e) {
            throw new TenantMigrationException(
                    "Flyway migration failed for schema '" + schema + "'", e);
        }
    }

    /**
     * Guards against SQL injection via schema name.
     * PostgreSQL schema names: alphanumeric + underscore, max 63 chars.
     */
    private String sanitize(String schema) {
        if (schema == null || schema.isBlank()) {
            throw new IllegalArgumentException("Tenant schema name must not be blank.");
        }
        String lower = schema.toLowerCase().trim();
        if (!lower.matches("[a-z][a-z0-9_]{0,62}")) {
            throw new IllegalArgumentException(
                    "Invalid schema name: '" + schema + "'. " +
                            "Must start with a letter, contain only lowercase letters, digits, underscores, " +
                            "and be at most 63 characters.");
        }
        return lower;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exception
    // ─────────────────────────────────────────────────────────────────────────

    public static class TenantMigrationException extends RuntimeException {
        public TenantMigrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Transactional
    private void seedCeoDetailsIntoTenant(List<GlobalUser> ceos) {

        for(GlobalUser gu : ceos){
            Role role = roleRepository.findByName("CEO")
                    .orElseThrow(() -> new RoleNotFoundException("Role Not Found !!"));

            User user = User.builder()
                    .email(gu.getEmail())
                    .fullName(gu.getFullName())
                    .username(gu.getUsername())
                    .passwordHash(gu.getPasswordHash())
                    .phoneNumber(gu.getPhoneNumber())
                    .isActive(true)
                    .role(role)
                    .build();

            User saved = userRepository.save(user);

            List<RolePermission> permissions = rolePermissionRepository.findByRoleId(saved.getRole().getId());
            List<UserPermission> userPermissions = new ArrayList<>();

            for(RolePermission rp : permissions){
                userPermissions.add(UserPermission.builder()
                        .action(rp.getAction())
                        .module(rp.getModule())
                        .user(saved)
                        .build()
                );
            }

            userPermissionRepository.saveAll(userPermissions);
        }
    }
}
