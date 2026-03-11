package com.security.rbac.modules.ceo.service;

import com.security.rbac.modules.ceo.exception.CeoFoundException;
import com.security.rbac.modules.tenant.exception.TenantFoundException;
import com.security.rbac.modules.tenant.exception.TenantNotFoundException;
import com.security.rbac.multitenancy.TenantContext;
import com.security.rbac.multitenancy.TenantMigrationService;
import com.security.rbac.modules.ceo.dto.request.SignUpRequest;
import com.security.rbac.modules.ceo.dto.response.SignUpResponse;
import com.security.rbac.modules.ceo.entity.GlobalUser;
import com.security.rbac.modules.ceo.repo.GlobalUserRepository;
import com.security.rbac.modules.tenant.dto.CreateTenantRequest;
import com.security.rbac.modules.tenant.entity.TenantRegistry;
import com.security.rbac.modules.tenant.repo.TenantRegistryRepository;
import com.security.rbac.modules.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * Orchestrates the full CEO + company sign-up flow.
 *
 * <h2>Steps</h2>
 * <ol>
 * <li>Derive a safe PostgreSQL schema name from {@code companyName}</li>
 * <li>Guard duplicate schema in {@code public.tenant_registry}</li>
 * <li>Guard duplicate email / username in {@code public.global_users}</li>
 * <li>Run Flyway tenant migrations (creates schema + tenant tables)</li>
 * <li>Persist {@code TenantRegistry} in public schema</li>
 * <li>Persist {@code GlobalUser} (CEO) in public schema</li>
 * </ol>
 *
 * <h2>TenantContext</h2>
 * All DB operations target the {@code public} schema — no context switching
 * needed.
 * {@link TenantContext} is explicitly set to {@code DEFAULT_TENANT} and cleared
 * in {@code finally} to be safe.
 */
@Service
@RequiredArgsConstructor
public class GlobalUserServiceImpl implements GlobalUserService {

    private static final Logger log = LoggerFactory.getLogger(GlobalUserServiceImpl.class);

    private final GlobalUserRepository globalUserRepository;
    private final TenantRegistryRepository tenantRegistryRepository;
    private final TenantMigrationService tenantMigrationService;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;

    @Override
    // ⚠️ NOT @Transactional on purpose — this method switches TenantContext
    // mid-execution (public → tenant schema). If a single Spring @Transactional
    // wraps everything, Hibernate reuses the same JDBC connection for the entire
    // method and the connection's search_path stays pinned to "public" even after
    // TenantContext.setCurrentTenant(tenantSchema) is called. By removing the
    // outer transaction boundary, each repository.save() call borrows a fresh
    // HikariCP connection and MultiTenantConnectionProviderImpl sets the correct
    // search_path for whatever TenantContext is active at that moment.
    public SignUpResponse signUp(SignUpRequest request) {

        TenantContext.setCurrentTenant(TenantContext.DEFAULT_TENANT);
        try {
            return doSignUp(request);
        } finally {
            TenantContext.clear();
        }
    }

    // ── Private orchestration ─────────────────────────────────────────────────
    private SignUpResponse doSignUp(SignUpRequest request) {

        String schemaName = generateTenantSchemaName(request.companyName());
        log.info("Sign-up initiated — company='{}' schema='{}'", request.companyName(), schemaName);

        // ── Guard duplicates ───────────────────────────────────────────────────
        if (tenantRegistryRepository.existsBySchemaName(schemaName)) {
            throw new TenantFoundException(
                    "Company with name '" + request.companyName() + "' is already registered.");
        }
        if (globalUserRepository.existsByEmail(request.email())) {
            throw new CeoFoundException(
                    "Email '" + request.email() + "' is already in use.");
        }
        if (globalUserRepository.existsByUsername(request.username())) {
            throw new CeoFoundException(
                    "Username '" + request.username() + "' is already taken.");
        }

        // ── Persist CEO in public.global_users ────────────────────────────────
        GlobalUser ceo = GlobalUser.builder()
                .email(request.email())
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phoneNumber(request.phoneNumber())
                .targetSchema(schemaName)
                .systemRole("CEO")
                .hasSchema(false)
                .isActive(true)
                .build();

        GlobalUser saved = globalUserRepository.save(ceo);
        log.info("CEO user created — id={} email='{}'", saved.getId(), saved.getEmail());

        // ── Create tenant schema + migrations ──────────────────────────────────
        tenantService.createTenantSubscription(schemaName, request.companyName());

        saved.setHasSchema(true);
        globalUserRepository.save(saved);
        log.info("CEO user has schema ");

        return new SignUpResponse(
                saved.getId(),
                saved.getFullName(),
                saved.getEmail(),
                saved.getUsername(),
                saved.getSystemRole(),
                saved.getCreatedAt(),
                "Company registered successfully. Welcome, " + saved.getFullName() + "!");
    }

    // ── Schema naming ─────────────────────────────────────────────────────────

    /**
     * Derives a safe PostgreSQL schema name from a company name.
     * "Acme Corp!" → "acme_corp"
     */
    private static String generateTenantSchemaName(String companyName) {
        String initials = get3Initials(companyName); // e.g., "MIC"
        String hash = shortHash6(companyName); // e.g., "91B2D0"
        return initials + "_SER_" + hash; // PG-safe (letters/digits/_ only)
    }

    private static String get3Initials(String companyName) {
        if (companyName == null)
            return "DEF";

        // Keep only letters and spaces
        String clean = companyName.replaceAll("[^a-zA-Z ]", " ").trim();
        if (clean.isEmpty())
            return "DEF";

        String[] words = clean.split("\\s+");

        StringBuilder sb = new StringBuilder(3);

        // If 3+ words: first letter of first 3 words
        if (words.length >= 3) {
            sb.append(Character.toUpperCase(words[0].charAt(0)));
            sb.append(Character.toUpperCase(words[1].charAt(0)));
            sb.append(Character.toUpperCase(words[2].charAt(0)));
            return sb.toString();
        }

        // If 2 words: first letters + next letter from first word (or X)
        if (words.length == 2) {
            sb.append(Character.toUpperCase(words[0].charAt(0)));
            sb.append(Character.toUpperCase(words[1].charAt(0)));
            sb.append(words[0].length() > 1 ? Character.toUpperCase(words[0].charAt(1)) : 'X');
            return sb.toString();
        }

        // 1 word: first 3 letters padded with X
        String w = words[0].toUpperCase();
        if (w.length() >= 3)
            return w.substring(0, 3);
        if (w.length() == 2)
            return w + "X";
        return w + "XX";
    }

    private static String shortHash6(String input) {
        if (input == null)
            input = "DEFAULT";

        CRC32 crc = new CRC32();
        crc.update(input.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));

        long v = crc.getValue(); // 32-bit
        String hex = Long.toHexString(v).toUpperCase();

        // left pad to 8, then take last 6 (stable length)
        hex = ("00000000" + hex).substring(hex.length() + 8 - 8);
        return hex.substring(2); // 6 chars
    }
}
