package com.security.rbac.modules.tenant.service;

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
import com.security.rbac.multitenancy.TenantContext;
import com.security.rbac.multitenancy.TenantMigrationService;
import com.security.rbac.modules.tenant.dto.CreateTenantRequest;
import com.security.rbac.modules.tenant.dto.TenantResponse;
import com.security.rbac.modules.tenant.entity.TenantRegistry;
import com.security.rbac.modules.tenant.repo.TenantRegistryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the tenant onboarding workflow:
 * <ol>
 * <li>Derive a safe PostgreSQL schema name from the tenant name.</li>
 * <li>Check the {@code public.tenant_registry} for duplicate schema names.</li>
 * <li>Delegate schema creation + Flyway migration to
 * {@link TenantMigrationService}.</li>
 * <li>Persist the new tenant in {@code public.tenant_registry}.</li>
 * </ol>
 *
 * <h2>Transactional strategy</h2>
 * <p>
 * The {@code @Transactional} boundary intentionally wraps only the registry
 * persistence step. The Flyway migration step
 * ({@link TenantMigrationService#migrateTenant})
 * is invoked <em>outside</em> a transaction because:
 * <ul>
 * <li>DDL in PostgreSQL is not transactional with respect to Flyway's own
 * internal metadata table writes.</li>
 * <li>We need the schema + tables to exist <em>before</em> committing the
 * tenant_registry row, so a rollback on failure is still possible for the
 * registry row.</li>
 * </ul>
 *
 * <h2>TenantContext during registry write</h2>
 * <p>
 * We must ensure {@link TenantContext} is set to
 * {@value TenantContext#DEFAULT_TENANT}
 * while writing to {@code public.tenant_registry}, because Hibernate's
 * {@code search_path} will otherwise target whatever the current thread-local
 * says.
 * Since {@link TenantRegistry} has {@code schema = "public"}, Hibernate
 * generates a
 * fully qualified SQL ({@code public.tenant_registry}), so TenantContext is NOT
 * strictly required here, but setting it makes the intent explicit and safe.
 */
@Service
@RequiredArgsConstructor
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRegistryRepository tenantRepo;
    private final TenantMigrationService migrationService;
    private final GlobalUserRepository globalUserRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new tenant: schema + migrations + registry entry.
     *
     * @param request tenant creation payload
     * @return the persisted {@link TenantResponse}
     * @throws TenantAlreadyExistsException if a tenant with the same schema already
     *                                      exists
     */
    public TenantResponse createTenant(CreateTenantRequest request) {
        String schemaName = deriveSchemaName(request.tenantName());

        log.info("Starting tenant provisioning for schema '{}'", schemaName);

        // ── Step 1: Guard against duplicates ─────────────────────────────────
        if (tenantRepo.existsBySchemaName(schemaName)) {
            throw new TenantAlreadyExistsException(
                    "Tenant with schema '" + schemaName + "' already exists.");
        }

        // ── Step 2: Run Flyway migration (DDL – outside JPA transaction) ──────
        // This creates the PostgreSQL schema and runs V1__init_tenant.sql etc.
        migrationService.migrateTenant(schemaName);

        // ── Step 3: Persist to public.tenant_registry ─────────────────────────
        TenantRegistry saved = saveRegistry(request, schemaName);

        // List<GlobalUser> ceos = globalUserRepository.findByTargetSchema(schemaName);
        // TenantContext.setCurrentTenant(schemaName);
        //
        // seedCeoDetailsIntoTenant(ceos);

        log.info("Tenant '{}' provisioned successfully (schema: '{}')",
                request.tenantName(), schemaName);

        return toResponse(saved);
    }

    /**
     * Creates a new tenant: schema + migrations + registry entry.
     *
     * @return the persisted {@link TenantResponse}
     * @throws TenantAlreadyExistsException if a tenant with the same schema already
     *                                      exists
     */
    public void createTenantSubscription(String schemaName, String companyName) {
        log.info("Starting tenant provisioning for schema '{}'", schemaName);

        // ── Step 1: Guard against duplicates ─────────────────────────────────
        if (tenantRepo.existsBySchemaName(schemaName)) {
            throw new TenantAlreadyExistsException(
                    "Tenant with schema '" + schemaName + "' already exists.");
        }

        // ── Step 2: Run Flyway migration (DDL – outside JPA transaction) ──────
        // This creates the PostgreSQL schema and runs V1__init_tenant.sql etc.
        migrationService.migrateTenant(schemaName);

        // ── Persist tenant registry ────────────────────────────────────────────
        TenantRegistry registry = saveRegistry(
                new CreateTenantRequest(companyName, companyName),
                schemaName);
        log.info("Tenant registry saved — id={}", registry.getId());

        List<GlobalUser> ceos = globalUserRepository.findByTargetSchema(schemaName);

        // Switch context to tenant schema BEFORE seeding so Hibernate uses the
        // correct search_path on its next connection borrow.
        TenantContext.setCurrentTenant(schemaName);
        try {
            seedCeoDetailsIntoTenant(ceos);
        } finally {
            // Always reset — caller (GlobalUserServiceImpl) sets it back to public
            // in its own finally block, but be safe here too.
            TenantContext.clear();
        }
    }

    // ⚠️ @Transactional on a private method is ignored by Spring AOP proxies.
    // The saves here work because TenantContext is already set to the tenant
    // schema by the caller, and each repository call borrows a fresh connection.
    private void seedCeoDetailsIntoTenant(List<GlobalUser> ceos) {

        for (GlobalUser gu : ceos) {
            Role role = roleRepository.findByName("CEO")
                    .orElseThrow(() -> new RoleNotFoundException("CEO role not found in tenant schema."));

            String fullName = gu.getFullName() != null ? gu.getFullName().trim() : "";
            int spaceIdx = fullName.indexOf(" ");
            String firstName = spaceIdx > 0 ? fullName.substring(0, spaceIdx) : (fullName.isEmpty() ? "CEO" : fullName);
            String lastName = spaceIdx > 0 ? fullName.substring(spaceIdx + 1) : "User";

            User user = User.builder()
                    .empId("CEO-" + gu.getId())
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(gu.getEmail())
                    .username(gu.getUsername())
                    .passwordHash(gu.getPasswordHash())
                    .contactNumber(gu.getPhoneNumber() != null ? gu.getPhoneNumber() : "0000000000")
                    .department("Management")
                    .designation("CEO")
                    .employmentType("Full-Time")
                    .dateOfJoining(java.time.LocalDate.now())
                    .currentAddressLine1("Not Provided")
                    .currentCity("Not Provided")
                    .currentState("Not Provided")
                    .currentCountry("Not Provided")
                    .currentPincode("000000")
                    .permanentAddressLine1("Not Provided")
                    .permanentCity("Not Provided")
                    .permanentState("Not Provided")
                    .permanentCountry("Not Provided")
                    .permanentPincode("000000")
                    .isActive(true)
                    .role(role)
                    .build();

            User saved = userRepository.save(user);

            List<RolePermission> permissions = rolePermissionRepository.findByRoleId(role.getId());
            List<UserPermission> userPermissions = new ArrayList<>();

            for (RolePermission rp : permissions) {
                userPermissions.add(UserPermission.builder()
                        .user(saved)
                        .module(rp.getModule())
                        .action(rp.getAction())
                        .allowed(true) // ← BUG FIX: was null → NOT NULL constraint
                        .build());
            }

            userPermissionRepository.saveAll(userPermissions);
            log.info("Seeded CEO user id={} with {} permissions into tenant schema.",
                    saved.getId(), userPermissions.size());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Derives a safe PostgreSQL schema name:
     * - lowercase
     * - spaces/hyphens → underscores
     * - strip non-alphanumeric chars (except underscore)
     * - ensure it starts with a letter
     */
    private String deriveSchemaName(String tenantName) {
        String schema = tenantName
                .toLowerCase()
                .trim()
                .replaceAll("[\\s\\-]+", "_")
                .replaceAll("[^a-z0-9_]", "");

        if (schema.isBlank() || !schema.matches("[a-z].*")) {
            schema = "t_" + schema;
        }
        if (schema.length() > 63) {
            schema = schema.substring(0, 63);
        }
        return schema;
    }

    /**
     * Persists the tenant registry entry in a dedicated transaction.
     * Uses REQUIRES_NEW so that a failure here does not affect the
     * already-committed
     * Flyway migration (which cannot be rolled back anyway).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TenantRegistry saveRegistry(CreateTenantRequest request, String schemaName) {
        TenantRegistry registry = TenantRegistry.builder()
                .tenantName(request.tenantName())
                .schemaName(schemaName)
                .displayName(request.displayName())
                .status("ACTIVE")
                .build();
        return tenantRepo.save(registry);
    }

    private TenantResponse toResponse(TenantRegistry r) {
        return new TenantResponse(
                r.getId(),
                r.getTenantName(),
                r.getSchemaName(),
                r.getDisplayName(),
                r.getStatus(),
                r.getCreatedAt());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exceptions
    // ─────────────────────────────────────────────────────────────────────────

    public static class TenantAlreadyExistsException extends RuntimeException {
        public TenantAlreadyExistsException(String message) {
            super(message);
        }
    }
}
