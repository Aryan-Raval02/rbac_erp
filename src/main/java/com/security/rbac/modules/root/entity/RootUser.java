package com.security.rbac.modules.root.entity;

import com.security.rbac.modules.role.enums.Roles;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.time.Instant;

/**
 * The single platform super-admin user stored in {@code public.root_user}.
 *
 * <p>
 * This entity always lives in the public schema regardless of tenant context.
 * It is created automatically at application startup from env variables
 * (see {@code RootUserInitializer}).
 */
@Entity
@Table(name = "root_user", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RootUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Login username (unique). */
    @Column(nullable = false, unique = true, length = 150)
    private String username;

    /** Email address (unique). */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt-hashed password — never stored in plain text. */
    @Column(nullable = false, length = 255)
    private String password;

    /** Display name. */
    @Column(length = 150)
    private String name;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Roles role;

    /** Whether this account is active. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
