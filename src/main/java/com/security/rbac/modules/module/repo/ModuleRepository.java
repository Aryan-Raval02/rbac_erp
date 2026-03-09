package com.security.rbac.modules.module.repo;

import com.security.rbac.modules.module.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link Module} — always accesses
 * {@code public.platform_modules}.
 */
@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {

    Optional<Module> findByName(String name);

    boolean existsByName(String name);
}
