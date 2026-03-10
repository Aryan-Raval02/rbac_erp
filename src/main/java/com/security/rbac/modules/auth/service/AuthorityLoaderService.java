package com.security.rbac.modules.auth.service;

import com.security.rbac.modules.userPermission.entity.UserPermission;
import com.security.rbac.modules.userPermission.repo.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorityLoaderService {

    private final UserPermissionRepository userPermissionRepository;

    @Transactional(readOnly = true)
    public Set<GrantedAuthority> loadAuthorities(String userType,
                                                 Long userId,
                                                 String tenantSchema,
                                                 String roleName) {
        Set<String> authorityNames = new HashSet<>();

        // Add base role authority
        if (roleName != null && !roleName.isBlank()) {
            authorityNames.add("ROLE_" + roleName.toUpperCase());
        }

        // Only tenant users need DB/cache permission resolution
        if ("TENANT".equals(userType)) {
            authorityNames.addAll(loadTenantAuthorityNames(userId, tenantSchema));
        }

        return authorityNames.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Cacheable(value = "tenant-authorities", key = "#tenantSchema + ':' + #userId")
    @Transactional(readOnly = true)
    public Set<String> loadTenantAuthorityNames(Long userId, String tenantSchema) {
        log.info("Loading authorities from DB for tenant={} userId={}", tenantSchema, userId);

        Set<String> authorityNames = new HashSet<>();

        List<UserPermission> userPerms = userId != null
                ? userPermissionRepository.findByUserId(userId)
                : List.of();

        for (UserPermission up : userPerms) {
            if (Boolean.TRUE.equals(up.getAllowed())) {
                String authority =
                        up.getModule().getName().toUpperCase() + "_"
                                + up.getAction().getName().toUpperCase();

                authorityNames.add(authority);
            }
        }

        return authorityNames;
    }

    @CacheEvict(value = "tenant-authorities", key = "#tenantSchema + ':' + #userId")
    public void evictTenantAuthorities(Long userId, String tenantSchema) {
        log.info("Evicted authority cache for tenant={} userId={}", tenantSchema, userId);
    }
}