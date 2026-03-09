package com.security.rbac.modules.ceo.repo;

import com.security.rbac.modules.ceo.entity.GlobalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link GlobalUser} — always targets
 * {@code public.global_users}.
 */
@Repository
public interface GlobalUserRepository extends JpaRepository<GlobalUser, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<GlobalUser> findByEmail(String email);

    Optional<GlobalUser> findByUsername(String username);

    List<GlobalUser> findByTargetSchema(String targetSchema);
}
