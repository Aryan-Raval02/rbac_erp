package com.security.rbac.jwt;

import com.security.rbac.multitenancy.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import com.security.rbac.modules.auth.service.AuthorityLoaderService;
import com.security.rbac.modules.auth.service.TokenBlacklistService;
import org.springframework.web.servlet.HandlerExceptionResolver;
import com.security.rbac.exception.InvalidTokenException;
import com.security.rbac.exception.TenantMismatchException;
import io.jsonwebtoken.JwtException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthorityLoaderService authorityLoaderService;
    private final TokenBlacklistService tokenBlacklistService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 1. Check if token exists
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            // 2. Query the token blacklist before processing anything
            if (tokenBlacklistService.isBlacklisted(jwt)) {
                log.warn("Attempted to use a blacklisted token");
                handlerExceptionResolver.resolveException(request, response, null,
                        new InvalidTokenException("Token has been revoked"));
                return;
            }

            // 3. Extract username & validate token (throws exception if expired/invalid)
            username = jwtService.extractUsername(jwt);

            // 4. Process authentication if it hasn't been done yet
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (jwtService.isTokenValid(jwt)) {
                    // Extract custom claims
                    Claims claims = jwtService.extractAllClaims(jwt);
                    String userType = claims.get("userType", String.class);
                    String role = claims.get("role", String.class);
                    String tenantSchema = claims.get("tenantSchema", String.class);
                    Long userId = claims.get("userId", Long.class);

                    // 5a. Cross-tenant spoofing protection
                    String headerTenant = request.getHeader("X-Tenant-ID");
                    if (tenantSchema != null && !tenantSchema.trim().isEmpty() && headerTenant != null
                            && !headerTenant.trim().isEmpty()) {
                        if (!tenantSchema.equals(headerTenant)) {
                            log.warn("Tenant mismatch! Token tenant: {}, Header tenant: {}", tenantSchema,
                                    headerTenant);
                            handlerExceptionResolver.resolveException(request, response, null,
                                    new TenantMismatchException("Cross-tenant spoofing attempt detected"));
                            return;
                        }
                    }

                    // 5b. Set TenantContext immediately (vital for downstream data access)
                    if (tenantSchema != null && !tenantSchema.trim().isEmpty()) {
                        TenantContext.setCurrentTenant(tenantSchema);
                        log.debug("TenantContext set to: {}", tenantSchema);
                    } else if ("ROOT".equals(userType)) {
                        TenantContext.setCurrentTenant(TenantContext.DEFAULT_TENANT); // public
                        log.debug("TenantContext set to public for ROOT");
                    }

                    // 6. Build Principal and Authorities
                    Set<GrantedAuthority> authorities = authorityLoaderService.loadAuthorities(
                            userType, userId, tenantSchema, role);

                    AuthUserPrincipal principal = AuthUserPrincipal.builder()
                            .userId(userId)
                            .username(username)
                            .userType(userType)
                            .tenantSchema(tenantSchema)
                            .roleName(role)
                            .active(true) // If token is valid, user is considered active in context
                            .authorities(authorities)
                            .build();

                    // 7. Create authToken using custom Principal
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            principal,
                            null, // No credentials needed for JWT principal
                            authorities);

                    // Embed extra HTTP request details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 8. Save to SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException e) {
            log.error("JWT Authentication failed: {}", e.getMessage());
            handlerExceptionResolver.resolveException(request, response, null,
                    new InvalidTokenException("Invalid or expired authentication token"));
            return;
        } catch (Exception e) {
            log.error("Unexpected error during JWT authentication: {}", e.getMessage());
            handlerExceptionResolver.resolveException(request, response, null, e);
            return;
        }

        // 9. Continue filter chain
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 10. CLEANUP - Prevent memory leaks and token poisoning in thread pool
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
