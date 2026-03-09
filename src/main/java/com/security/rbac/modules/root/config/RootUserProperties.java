package com.security.rbac.modules.root.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code root.*} properties from application.yaml / environment.
 *
 * <p>
 * Set these in your {@code .env} file:
 * 
 * <pre>
 *   ROOT_EMAIL=admin@example.com
 *   ROOT_PASSWORD=StrongPass123!
 *   ROOT_NAME=Platform Admin
 *   ROOT_USERNAME=root_admin
 * </pre>
 *
 * <p>
 * application.yaml maps them as:
 * 
 * <pre>
 *   root:
 *     email: ${ROOT_EMAIL}
 *     password: ${ROOT_PASSWORD}
 *     name: ${ROOT_NAME}
 *     username: ${ROOT_USERNAME}
 * </pre>
 */
@ConfigurationProperties(prefix = "root")
public record RootUserProperties(
        String email,
        String password,
        String name,
        String username) {
}
