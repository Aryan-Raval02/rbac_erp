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

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http,
                        TenantResolverFilter tenantResolverFilter)
                        throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // ── Public endpoints ──────────────────────────────────────────
                                                .requestMatchers(
                                                                "/api/v1/**", // tenant onboarding (restrict to
                                                                                   // SUPER_ADMIN in prod)
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**",
                                                                "/actuator/health")
                                                .permitAll()
                                                // ── Everything else requires authentication ────────────────────
                                                .anyRequest().authenticated())
                                // Register TenantResolverFilter before Spring Security's auth filter
                                // so TenantContext is populated before any security decision is made.
                                .addFilterBefore(tenantResolverFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
