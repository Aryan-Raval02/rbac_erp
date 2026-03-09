package com.security.rbac.modules.rolePermission.service;

import com.security.rbac.modules.action.entity.Action;
import com.security.rbac.modules.action.exception.ActionNotFoundException;
import com.security.rbac.modules.action.repo.ActionRepository;
import com.security.rbac.modules.module.entity.Module;
import com.security.rbac.modules.module.exception.ModuleNotFoundException;
import com.security.rbac.modules.module.repo.ModuleRepository;
import com.security.rbac.modules.role.entity.Role;
import com.security.rbac.modules.role.exception.RoleNotFoundException;
import com.security.rbac.modules.role.repo.RoleRepository;
import com.security.rbac.modules.rolePermission.dto.request.AssignRolePermissionRequest;
import com.security.rbac.modules.rolePermission.dto.response.RolePermissionResponse;
import com.security.rbac.modules.rolePermission.entity.RolePermission;
import com.security.rbac.modules.rolePermission.repo.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles assignment and retrieval of role permissions.
 *
 * <h2>Duplicate Handling</h2>
 * If a (role, module, action) mapping already exists, it is silently skipped
 * (idempotent). The response tells the caller how many were saved vs skipped.
 */
@Service
@RequiredArgsConstructor
public class RolePermissionServiceImpl implements RolePermissionService {

    private static final Logger log = LoggerFactory.getLogger(RolePermissionServiceImpl.class);

    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final ModuleRepository moduleRepository;
    private final ActionRepository actionRepository;

    /**
     * Assigns a list of module+action permissions to a role.
     * <ul>
     * <li>Validates that the role, each module, and each action exist.</li>
     * <li>Skips entries that already exist (unique constraint guard).</li>
     * <li>Returns a summary with saved/skipped counts + full permission list.</li>
     * </ul>
     */
    @Transactional
    @Override
    public RolePermissionResponse assignPermissions(AssignRolePermissionRequest request) {

        // 1 — Validate role
        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new RoleNotFoundException(
                        "Role not found with id: " + request.roleId()));

        int saved = 0;
        int skipped = 0;

        // 2 — Process each permission entry
        for (AssignRolePermissionRequest.PermissionEntry entry : request.permissions()) {

            // Validate module
            Module module = moduleRepository.findById(entry.moduleId())
                    .orElseThrow(() -> new ModuleNotFoundException(
                            "Module not found with id: " + entry.moduleId()));

            // Validate action
            Action action = actionRepository.findById(entry.actionId())
                    .orElseThrow(() -> new ActionNotFoundException(
                            "Action not found with id: " + entry.actionId()));

            // Check for existing mapping (idempotent insert)
            boolean exists = rolePermissionRepository
                    .findByRoleIdAndModuleIdAndActionId(role.getId(), module.getId(), action.getId())
                    .isPresent();

            if (exists) {
                log.debug("Permission already exists — skipping role={} module={} action={}",
                        role.getId(), module.getId(), action.getId());
                skipped++;
                continue;
            }

            RolePermission permission = RolePermission.builder()
                    .role(role)
                    .module(module)
                    .action(action)
                    .allowed(true)
                    .build();

            rolePermissionRepository.save(permission);
            saved++;
        }

        log.info("Role {} — {} permission(s) saved, {} skipped.", role.getName(), saved, skipped);

        // 3 — Build response with full current permission list for this role
        List<RolePermissionResponse.PermissionDetail> details = buildPermissionDetails(role.getId());

        return new RolePermissionResponse(role.getId(), saved, skipped, details);
    }

    /**
     * Returns all current permissions for a role.
     */
    @Transactional(readOnly = true)
    @Override
    public RolePermissionResponse getPermissions(Long roleId) {
        roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found with id: " + roleId));

        List<RolePermissionResponse.PermissionDetail> details = buildPermissionDetails(roleId);
        return new RolePermissionResponse(roleId, 0, 0, details);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<RolePermissionResponse.PermissionDetail> buildPermissionDetails(Long roleId) {
        List<RolePermission> permissions = rolePermissionRepository.findByRoleId(roleId);
        List<RolePermissionResponse.PermissionDetail> details = new ArrayList<>();
        for (RolePermission p : permissions) {
            details.add(new RolePermissionResponse.PermissionDetail(
                    p.getId(),
                    p.getModule().getId(),
                    p.getModule().getName(),
                    p.getAction().getId(),
                    p.getAction().getName(),
                    p.getAllowed()));
        }
        return details;
    }
}
