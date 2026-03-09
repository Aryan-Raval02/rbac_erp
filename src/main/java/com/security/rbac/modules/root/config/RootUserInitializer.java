package com.security.rbac.modules.root.config;

import com.security.rbac.modules.role.enums.Roles;
import com.security.rbac.multitenancy.TenantContext;
import com.security.rbac.modules.root.entity.RootUser;
import com.security.rbac.modules.root.repo.RootUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs once at application startup and creates the root user in
 * {@code public.root_user} if it does not already exist.
 *
 * <h2>Idempotent</h2>
 * Checks by email before inserting — safe to restart the application multiple
 * times without duplicating the root user.
 *
 * <h2>Tenant context</h2>
 * Explicitly sets {@code TenantContext} to {@code "public"} before any JPA
 * call so Hibernate targets the correct schema, then restores it in a
 * {@code finally} block.
 *
 * <h2>Password security</h2>
 * The plain-text password from env vars is encoded with BCrypt before
 * persisting — it is never stored in plain text.
 */
@Component
public class RootUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RootUserInitializer.class);

    private final RootUserRepository rootUserRepository;
    private final RootUserProperties rootUserProperties;
    private final PasswordEncoder passwordEncoder;

    public RootUserInitializer(RootUserRepository rootUserRepository,
            RootUserProperties rootUserProperties,
            PasswordEncoder passwordEncoder) {
        this.rootUserRepository = rootUserRepository;
        this.rootUserProperties = rootUserProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Force public schema for this operation
        TenantContext.setCurrentTenant(TenantContext.DEFAULT_TENANT);
        try {
            createRootUserIfAbsent();
        } finally {
            TenantContext.clear();
        }
    }

    private void createRootUserIfAbsent() {
        String email = rootUserProperties.email();
        String username = rootUserProperties.username();

        if (rootUserRepository.existsByEmail(email)) {
            log.info("Root user already exists (email={}), skipping creation.", email);
            return;
        }

        RootUser rootUser = RootUser.builder()
                .email(email)
                .username(username)
                .role(Roles.SERAVION)
                .name(rootUserProperties.name())
                .password(passwordEncoder.encode(rootUserProperties.password()))
                .active(true)
                .build();

        rootUserRepository.save(rootUser);
        log.info("Root user created successfully (email={}, username={}).", email, username);
    }
}
