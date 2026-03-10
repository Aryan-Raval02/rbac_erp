package com.security.rbac.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Custom Principal object stored in the SecurityContextHolder.
 * Implements UserDetails for seamless integration with Spring Security.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserPrincipal implements UserDetails {
    private Long userId;
    private String username;
    private String userType;
    private String tenantSchema;
    private String roleName;
    private boolean active;
    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // JWT based authentication, no password needed in principal
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return active;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
