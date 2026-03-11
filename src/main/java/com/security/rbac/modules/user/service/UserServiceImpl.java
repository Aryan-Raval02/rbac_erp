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
import com.security.rbac.modules.user.dto.UserMapper;
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
        private final UserMapper userMapper;

        @Override
        @Transactional
        public UserResponse createUser(CreateUserRequest request) {

                // Duplicate guards
                if (userRepository.existsByEmail(request.email())) {
                        throw new UserAlreadyExistsException(
                                        "Email '" + request.email() + "' is already in use in this tenant.");
                }
                if (userRepository.existsByUsername(request.username())) {
                        throw new UserAlreadyExistsException(
                                        "Username '" + request.username() + "' is already taken in this tenant.");
                }
                if (userRepository.existsByEmpId(request.empId())) {
                        throw new UserAlreadyExistsException(
                                        "Employee ID '" + request.empId() + "' is already taken in this tenant.");
                }

                // Validate role
                Role role = roleRepository.findById(request.roleId())
                                .orElseThrow(() -> new RoleNotFoundException(
                                                "Role not found with id: " + request.roleId()));

                User reportingManager = null;
                if (request.reportingManagerId() != null) {
                        reportingManager = userRepository.findById(request.reportingManagerId())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Reporting Manager not found with id: "
                                                                        + request.reportingManagerId()));
                }

                User user = userMapper.toEntity(request);

                user.setPasswordHash(passwordEncoder.encode(request.password()));
                user.setRole(role);
                user.setReportingManager(reportingManager);

                User saved = userRepository.save(user);
                log.info("User created — id={}, email='{}', role='{}'", saved.getId(), saved.getEmail(),
                                role.getName());

                // Persist permissions (if provided)
                List<CreateUserRequest.PermissionRequest> permissions = request.permissions() != null
                                ? request.permissions()
                                : Collections.emptyList();
                if (!permissions.isEmpty()) {
                        savePermissions(saved, permissions);
                }

                return toResponse(saved);
        }

        private void savePermissions(User user, List<CreateUserRequest.PermissionRequest> permissions) {
                int saved = 0;
                int skipped = 0;

                for (CreateUserRequest.PermissionRequest perm : permissions) {
                        Module module = moduleRepository.findById(perm.moduleId())
                                        .orElseThrow(() -> new ModuleNotFoundException(
                                                        "Module not found with id: " + perm.moduleId()));

                        Action action = actionRepository.findById(perm.actionId())
                                        .orElseThrow(() -> new ActionNotFoundException(
                                                        "Action not found with id: " + perm.actionId()));

                        boolean exists = userPermissionRepository
                                        .findByUserIdAndModuleIdAndActionId(user.getId(), module.getId(),
                                                        action.getId())
                                        .isPresent();

                        if (exists) {
                                log.debug("UserPermission exists — skipping user={} module={} action={}", user.getId(),
                                                module.getId(), action.getId());
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

        private UserResponse toResponse(User u) {
                return userMapper.toResponse(u);
        }
}
