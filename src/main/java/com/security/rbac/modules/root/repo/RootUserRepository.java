package com.security.rbac.modules.root.repo;

import com.security.rbac.modules.root.entity.RootUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link RootUser} — always accesses the {@code public} schema.
 *
 * <p>
 * Callers must ensure TenantContext is set to "public" before use,
 * or rely on {@code RootUserInitializer} which sets it explicitly.
 */
@Repository
public interface RootUserRepository extends JpaRepository<RootUser, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsernameAndEmail(String username, String email);

    Optional<RootUser> findByEmail(String email);

    Optional<RootUser> findByUsername(String username);
}
