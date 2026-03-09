package com.security.rbac.modules.user.entity;

import com.security.rbac.modules.role.entity.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Tenant-scoped user stored in the active tenant's {@code users} table.
 *
 * <p>
 * No {@code schema = "..."} in {@link Table} — Hibernate uses the
 * tenant-specific {@code search_path} set by
 * {@code MultiTenantConnectionProviderImpl}. Every operation on this entity
 * automatically targets the correct tenant schema.
 *
 * <p>
 * Maps exactly to {@code V3__users_table.sql}.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_username", columnNames = "username")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    /** BCrypt-hashed password — never stored or returned in plain text. */
    @Column(name = "password_hash", nullable = false, length = 512)
    private String passwordHash;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    /**
     * FK → {@code roles.id} in the same tenant schema.
     * LAZY fetch — only load when the role is accessed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_users_role"))
    private Role role;

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
