package com.security.rbac.security;

import com.security.rbac.jwt.AuthUserPrincipal;
import com.security.rbac.modules.module.repo.ModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Custom PermissionEvaluator that bridges @PreAuthorize("hasPermission(...)")
 * to our dynamic MODULE_ACTION authorities.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RbacPermissionEvaluator implements PermissionEvaluator {

    private final ModuleRepository moduleRepository;

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        if (auth == null || permission == null) {
            return false;
        }

        AuthUserPrincipal principal = getPrincipal(auth);
        if (principal == null) {
            return false;
        }

        // 1. Bypass check for ROOT and CEO (Platform-level admins)
        if ("ROOT".equals(principal.getUserType()) || "CEO".equals(principal.getUserType())) {
            log.trace("Bypassing permission check for {} user", principal.getUserType());
            return true;
        }

        // 2. Logic for specific domain objects (e.g. Module, User)
        // If targetDomainObject is a String (module name), check authority directly
        if (targetDomainObject instanceof String) {
            String requiredAuthority = (targetDomainObject + "_" + permission).toUpperCase();
            return hasAuthority(auth, requiredAuthority);
        }

        return false;
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        if (auth == null || targetType == null || permission == null) {
            return false;
        }

        AuthUserPrincipal principal = getPrincipal(auth);
        if (principal == null) {
            return false;
        }

        // 1. Bypass check for ROOT and CEO
        if ("ROOT".equals(principal.getUserType()) || "CEO".equals(principal.getUserType())) {
            return true;
        }

        // 2. Dynamic resolution of Module names from ID
        if ("MODULE".equalsIgnoreCase(targetType) && targetId instanceof Long) {
            return moduleRepository.findById((Long) targetId)
                    .map(module -> {
                        String requiredAuthority = (module.getName() + "_" + permission).toUpperCase();
                        return hasAuthority(auth, requiredAuthority);
                    })
                    .orElse(false);
        }

        return false;
    }

    private AuthUserPrincipal getPrincipal(Authentication auth) {
        if (auth.getPrincipal() instanceof AuthUserPrincipal) {
            return (AuthUserPrincipal) auth.getPrincipal();
        }
        return null;
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase(authority));
    }
}
