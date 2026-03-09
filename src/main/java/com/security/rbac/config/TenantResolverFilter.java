package com.security.rbac.config;

import com.security.rbac.multitenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * Servlet filter that resolves the tenant schema from each incoming HTTP
 * request
 * and stores it in {@link TenantContext} for the duration of the request.
 *
 * <p>
 * <b>Tenant resolution strategy</b> (in priority order):
 * <ol>
 * <li>{@code X-Tenant-ID} HTTP header</li>
 * <li>Subdomain — e.g. {@code acme.yourdomain.com} → schema {@code acme}</li>
 * <li>Default to {@value TenantContext#DEFAULT_TENANT} (public schema)</li>
 * </ol>
 *
 * <p>
 * <b>IMPORTANT — NOT annotated @Component.</b>
 * This filter is declared as a {@code @Bean} inside {@link SecurityConfig}
 * and registered via {@code addFilterBefore}. If it were also annotated
 * {@code @Component}, Spring Boot would <em>additionally</em> register it in
 * the Servlet filter chain, causing it to execute twice per request.
 *
 * <p>
 * <b>Thread-local safety:</b> {@link TenantContext#clear()} is always
 * called in the {@code finally} block even if downstream filters throw.
 */
public class TenantResolverFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantResolverFilter.class);
    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String tenantId = resolveTenant(request);
            log.debug("Resolved tenant '{}' for request: {}", tenantId, request.getRequestURI());
            TenantContext.setCurrentTenant(tenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear(); // CRITICAL: prevent thread-local leak on pooled threads
        }
    }

    // ── Resolution logic — extend as needed ──────────────────────────────────

    private String resolveTenant(HttpServletRequest request) {
        // Priority 1: explicit header
        String header = request.getHeader(TENANT_HEADER);
        if (header != null && !header.isBlank()) {
            return header.toLowerCase().trim();
        }

        // Priority 2: subdomain (acme.yourdomain.com → acme)
        String host = request.getServerName();
        if (host != null) {
            String[] parts = host.split("\\.");
            if (parts.length > 2) {
                String subdomain = parts[0].toLowerCase();
                if (!subdomain.equals("www") && !subdomain.equals("api")) {
                    return subdomain;
                }
            }
        }

        // Priority 3: fall back to public schema
        return TenantContext.DEFAULT_TENANT;
    }
}
