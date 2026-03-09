package com.security.rbac.modules.ceo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Platform-level (global) user stored in {@code public.global_users}.
 *
 * <p>
 * Created during company sign-up. Represents the CEO/owner of a tenant.
 * {@code targetSchema} stores the tenant schema this user belongs to.
 */
@Entity
@Table(name = "global_users", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    /** BCrypt-hashed password — never stored plain. */
    @Column(name = "password_hash", nullable = false, length = 512)
    private String passwordHash;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    /** The tenant PostgreSQL schema this user belongs to. */
    @Column(name = "target_schema", length = 63)
    private String targetSchema;

    /** Platform role — default CEO for sign-up registrations. */
    @Column(name = "system_role", length = 50)
    @Builder.Default
    private String systemRole = "CEO";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

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
