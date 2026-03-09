package com.security.rbac.modules.action.repo;

import com.security.rbac.modules.action.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link Action} — always accesses
 * {@code public.platform_actions}.
 */
@Repository
public interface ActionRepository extends JpaRepository<Action, Long> {

    Optional<Action> findByName(String name);

    boolean existsByName(String name);
}
