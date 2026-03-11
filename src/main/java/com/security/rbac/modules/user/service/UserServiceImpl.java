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

                // Persist user
                User user = User.builder()
                                .empId(request.empId())
                                .firstName(request.firstName())
                                .lastName(request.lastName())
                                .email(request.email())
                                .username(request.username())
                                .passwordHash(passwordEncoder.encode(request.password()))
                                .contactNumber(request.contactNumber())
                                .alternateNumber(request.alternateNumber())
                                .department(request.department())
                                .designation(request.designation())
                                .role(role)
                                .reportingManager(reportingManager)
                                .employmentType(request.employmentType())
                                .dateOfJoining(request.dateOfJoining())
                                .status(request.status() != null ? request.status() : "ACTIVE")
                                .isActive(true)
                                .currentAddressLine1(request.currentAddressLine1())
                                .currentAddressLine2(request.currentAddressLine2())
                                .currentCity(request.currentCity())
                                .currentState(request.currentState())
                                .currentCountry(request.currentCountry())
                                .currentPincode(request.currentPincode())
                                .permanentAddressLine1(request.permanentAddressLine1())
                                .permanentAddressLine2(request.permanentAddressLine2())
                                .permanentCity(request.permanentCity())
                                .permanentState(request.permanentState())
                                .permanentCountry(request.permanentCountry())
                                .permanentPincode(request.permanentPincode())
                                .build();

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
                return new UserResponse(
                                u.getId(),
                                u.getEmpId(),
                                u.getFirstName(),
                                u.getLastName(),
                                u.getEmail(),
                                u.getUsername(),
                                u.getContactNumber(),
                                u.getAlternateNumber(),
                                u.getDepartment(),
                                u.getDesignation(),
                                u.getRole().getId(),
                                u.getRole().getName(),
                                u.getReportingManager() != null ? u.getReportingManager().getId() : null,
                                u.getEmploymentType(),
                                u.getDateOfJoining(),
                                u.getStatus(),
                                u.getIsActive(),
                                u.getCurrentAddressLine1(),
                                u.getCurrentAddressLine2(),
                                u.getCurrentCity(),
                                u.getCurrentState(),
                                u.getCurrentCountry(),
                                u.getCurrentPincode(),
                                u.getPermanentAddressLine1(),
                                u.getPermanentAddressLine2(),
                                u.getPermanentCity(),
                                u.getPermanentState(),
                                u.getPermanentCountry(),
                                u.getPermanentPincode(),
                                u.getCreatedAt());
        }
}
