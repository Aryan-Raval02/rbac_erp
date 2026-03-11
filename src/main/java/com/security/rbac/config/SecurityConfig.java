package com.security.rbac.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.PermissionEvaluator;
import com.security.rbac.security.RbacPermissionEvaluator;
import com.security.rbac.modules.module.repo.ModuleRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration — stateless, JWT-ready.
 *
 * <p>
 * <b>Filter registration:</b> {@link TenantResolverFilter} is declared here
 * as a {@code @Bean} and not annotated with {@code @Component}. Declaring it as
 * a {@code @Bean} while also using {@code addFilterBefore} would NOT cause
 * double-registration because Spring Security tracks filters registered via
 * its DSL separately from the Servlet container chain. However, if the filter
 * were a {@code @Component} it <em>would</em> also be auto-registered by Boot,
 * causing it to run twice — hence the explicit bean-only approach.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * Declares {@link TenantResolverFilter} as a Spring bean so it can be
     * injected into {@link #securityFilterChain} and benefits from full
     * dependency injection if extended later.
     */
    @Bean
    public TenantResolverFilter tenantResolverFilter() {
        return new TenantResolverFilter();
    }

    /**
     * BCrypt password encoder — used by RootUserInitializer and future auth layer.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Registers our custom PermissionEvaluator
     * for @PreAuthorize("hasPermission(...)").
     */
    @Bean
    public PermissionEvaluator permissionEvaluator(ModuleRepository moduleRepository) {
        return new RbacPermissionEvaluator(moduleRepository);
    }

    /**
     * Configures the expression handler to use our custom PermissionEvaluator.
     * Must be static to avoid initialization order issues
     * with @EnableMethodSecurity.
     */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            PermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(permissionEvaluator);
        return expressionHandler;
    }

    /**
     * Disables the default Spring Boot Security auto-configured user
     * (the one that generates a random password in the console on startup)
     * because we use custom auth services and JWT instead.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(
                    "Default UserDetailsService is disabled. Custom JWT authentication in use.");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   TenantResolverFilter tenantResolverFilter,
                                                   com.security.rbac.jwt.JwtAuthFilter jwtAuthFilter)
            throws Exception {

        http
                // Enable CORS for frontend integration.
                // This allows browser clients (React / Vite / frontend apps)
                // to call backend APIs from allowed origins and send:
                // - Authorization header (JWT)
                // - X-Tenant-ID header (tenant routing)
                // The configuration source is defined in corsConfigurationSource().
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Disable CSRF because this application uses stateless JWT authentication
                // CSRF protection is mainly required for session-based browser authentication
                .csrf(AbstractHttpConfigurer::disable)

                // Configure stateless session management
                // Spring Security will not create or use HttpSession
                // Every request must carry JWT token explicitly
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure endpoint authorization rules
                .authorizeHttpRequests(auth -> auth

                        // Public endpoints accessible without authentication
                        .requestMatchers(
                                "/api/v1/auth/root/login", // Root user login
                                "/api/v1/auth/ceo/login", // CEO login
                                "/api/v1/auth/login", // Tenant user login
                                "/api/v1/auth/refresh", // Refresh token endpoint
                                "/api/v1/auth/signup", // CEO tenant registration/signup
                                "/api/v1/auth/me",

                                // Swagger/OpenAPI documentation endpoints
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",

                                // Health check endpoint
                                "/actuator/health")
                        .permitAll()

                        // All remaining endpoints require authenticated JWT token
                        .anyRequest().authenticated())

                // Filter 1: Resolve tenant before JWT authentication
                // Reads X-Tenant-ID header and stores schema in TenantContext
                .addFilterBefore(
                        tenantResolverFilter,
                        UsernamePasswordAuthenticationFilter.class)

                // Filter 2: JWT authentication filter executes after tenant resolution
                // Validates token, extracts claims, sets SecurityContext,
                // and overrides TenantContext using token tenant claim when required
                .addFilterAfter(
                        jwtAuthFilter,
                        TenantResolverFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Tenant-ID"
        ));

        config.setExposedHeaders(List.of(
                "Authorization",
                "X-Tenant-ID"
        ));

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
