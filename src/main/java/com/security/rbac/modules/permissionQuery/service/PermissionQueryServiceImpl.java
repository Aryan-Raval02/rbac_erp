package com.security.rbac.modules.permissionQuery.service;

import com.security.rbac.modules.auth.dto.response.AuthResponse;
import com.security.rbac.modules.module.entity.Module;
import com.security.rbac.modules.user.entity.User;
import com.security.rbac.modules.user.repo.UserRepository;
import com.security.rbac.modules.userPermission.entity.UserPermission;
import com.security.rbac.modules.userPermission.repo.UserPermissionRepository;
import com.security.rbac.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionQueryServiceImpl implements PermissionQueryService {

    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;

    @Override
    public List<AuthResponse.PermissionModuleDto> getGroupedPermissions(Long globalUserId, String username,
                                                                              String tenantSchema) {

        if (tenantSchema == null || tenantSchema.isBlank()) {
            return null;
        }

        try {
            TenantContext.setCurrentTenant(tenantSchema);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            List<UserPermission> permissions = userPermissionRepository.findByUserIdAndAllowedTrue(user.getId());

            Map<Long, List<UserPermission>> permissionsByModuleId = permissions.stream()
                    .filter(up -> up.getModule() != null)
                    .collect(Collectors.groupingBy(up -> up.getModule().getId()));

            return permissionsByModuleId.entrySet().stream()
                    .map(entry -> {
                        List<UserPermission> modulePermissions = entry.getValue();
                        UserPermission first = modulePermissions.get(0);

                        List<AuthResponse.ActionDto> actionDtos = modulePermissions.stream()
                                .filter(up -> up.getAction() != null)
                                .map(up -> new AuthResponse.ActionDto(
                                        up.getAction().getId(),
                                        up.getAction().getName(),
                                        up.getAction().getLabel()
                                ))
                                .distinct()
                                .toList();

                        return new AuthResponse.PermissionModuleDto(
                                first.getModule().getId(),
                                first.getModule().getName(),
                                first.getModule().getLabel(),
                                actionDtos
                        );
                    })
                    .toList();
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Failed to load CEO permissions for username: " + username +
                            " in schema: " + tenantSchema,
                    ex
            );
        } finally {
            TenantContext.clear();
        }
    }
}
