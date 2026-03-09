package com.security.rbac.modules.user.service;

import com.security.rbac.modules.action.entity.Action;
import com.security.rbac.modules.action.exception.ActionNotFoundException;
import com.security.rbac.modules.action.repo.ActionRepository;
import com.security.rbac.modules.module.entity.Module;
import com.security.rbac.modules.module.exception.ModuleNotFoundException;
import com.security.rbac.modules.module.repo.ModuleRepository;
import com.security.rbac.modules.role.entity.Role;
import com.security.rbac.modules.role.exception.RoleNotFoundException;
import com.security.rbac.modules.role.repo.RoleRepository;
import com.security.rbac.modules.user.dto.request.CreateUserRequest;
import com.security.rbac.modules.user.dto.response.UserResponse;
import com.security.rbac.modules.user.entity.User;
import com.security.rbac.modules.user.exception.UserAlreadyExistsException;
import com.security.rbac.modules.user.repo.UserRepository;
import com.security.rbac.modules.userPermission.entity.UserPermission;
import com.security.rbac.modules.userPermission.repo.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Tenant-scoped user service.
 *
 * <p>
 * All DB operations automatically target the tenant schema because
 * Hibernate resolves the {@code search_path} from {@code TenantContext}
 * (set by {@code TenantResolverFilter} per request) — no explicit schema
 * switching needed here.
 *
 * <h2>Create-User Flow</h2>
 * <ol>
 * <li>Guard duplicate email / username within the tenant</li>
 * <li>Validate the assigned role exists in the tenant</li>
 * <li>BCrypt-hash the password and persist the user</li>
 * <li>If permissions are supplied, persist them via
 * {@link #savePermissions}</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

        private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final ModuleRepository moduleRepository;
        private final ActionRepository actionRepository;
        private final UserPermissionRepository userPermissionRepository;
        private final PasswordEncoder passwordEncoder;

        // ═════════════════════════════════════════════════════════════════════════
        // Public API
        // ═════════════════════════════════════════════════════════════════════════

        @Override
        @Transactional
        public UserResponse createUser(CreateUserRequest request) {

                // ── 1. Duplicate guards ───────────────────────────────────────────────
                if (userRepository.existsByEmail(request.email())) {
                        throw new UserAlreadyExistsException(
                                        "Email '" + request.email() + "' is already in use in this tenant.");
                }
                if (userRepository.existsByUsername(request.username())) {
                        throw new UserAlreadyExistsException(
                                        "Username '" + request.username() + "' is already taken in this tenant.");
                }

                // ── 2. Validate role ──────────────────────────────────────────────────
                Role role = roleRepository.findById(request.roleId())
                                .orElseThrow(() -> new RoleNotFoundException(
                                                "Role not found with id: " + request.roleId()));

                // ── 3. Persist user ───────────────────────────────────────────────────
                User user = User.builder()
                                .fullName(request.fullName())
                                .email(request.email())
                                .username(request.username())
                                .passwordHash(passwordEncoder.encode(request.password()))
                                .phoneNumber(request.phoneNumber())
                                .role(role)
                                .isActive(true)
                                .build();

                User saved = userRepository.save(user);
                log.info("User created — id={}, email='{}', role='{}'",
                                saved.getId(), saved.getEmail(), role.getName());

                // ── 4. Persist permissions (if provided) ──────────────────────────────
                List<CreateUserRequest.PermissionRequest> permissions = request.permissions() != null
                                ? request.permissions()
                                : Collections.emptyList();

                if (!permissions.isEmpty()) {
                        savePermissions(saved, permissions);
                }

                return toResponse(saved);
        }

        // ═════════════════════════════════════════════════════════════════════════
        // Private helpers
        // ═════════════════════════════════════════════════════════════════════════

        /**
         * Persists user-level permission overrides into {@code user_permissions}.
         *
         * <p>
         * Skips duplicates silently (idempotent) — guards against the unique
         * constraint {@code uk_up_user_module_action} on (user_id, module_id,
         * action_id).
         *
         * @param user        the newly created user
         * @param permissions list of module+action pairs from the request
         */
        private void savePermissions(User user,
                        List<CreateUserRequest.PermissionRequest> permissions) {
                int saved = 0;
                int skipped = 0;

                for (CreateUserRequest.PermissionRequest perm : permissions) {

                        // Validate module
                        Module module = moduleRepository.findById(perm.moduleId())
                                        .orElseThrow(() -> new ModuleNotFoundException(
                                                        "Module not found with id: " + perm.moduleId()));

                        // Validate action
                        Action action = actionRepository.findById(perm.actionId())
                                        .orElseThrow(() -> new ActionNotFoundException(
                                                        "Action not found with id: " + perm.actionId()));

                        // Skip if already exists (idempotent guard)
                        boolean exists = userPermissionRepository
                                        .findByUserIdAndModuleIdAndActionId(user.getId(), module.getId(),
                                                        action.getId())
                                        .isPresent();

                        if (exists) {
                                log.debug("UserPermission exists — skipping user={} module={} action={}",
                                                user.getId(), module.getId(), action.getId());
                                skipped++;
                                continue;
                        }

                        userPermissionRepository.save(
                                        UserPermission.builder()
                                                        .user(user)
                                                        .module(module)
                                                        .action(action)
                                                        .allowed(true)
                                                        .build());
                        saved++;
                }

                log.info("Permissions saved={} skipped={} for user id={}", saved, skipped, user.getId());
        }

        /**
         * Maps a {@link User} entity to a {@link UserResponse} DTO.
         * Password is intentionally excluded.
         */
        private UserResponse toResponse(User u) {
                return new UserResponse(
                                u.getId(),
                                u.getFullName(),
                                u.getEmail(),
                                u.getUsername(),
                                u.getPhoneNumber(),
                                u.getRole().getId(),
                                u.getRole().getName(),
                                u.getIsActive(),
                                u.getCreatedAt());
        }
}
