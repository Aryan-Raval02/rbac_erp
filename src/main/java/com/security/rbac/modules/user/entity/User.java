package com.security.rbac.modules.user.entity;

import com.security.rbac.modules.role.entity.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Tenant-scoped user stored in the active tenant's {@code users} table.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_users_emp_id", columnNames = "emp_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emp_id", nullable = false, unique = true, length = 50)
    private String empId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(unique = true, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 512)
    private String passwordHash;

    @Column(name = "contact_number", nullable = false, length = 15)
    private String contactNumber;

    @Column(name = "alternate_number", length = 15)
    private String alternateNumber;

    @Column(nullable = false, length = 150)
    private String department;

    @Column(nullable = false, length = 150)
    private String designation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_users_role"))
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_manager_id", foreignKey = @ForeignKey(name = "fk_users_reporting_manager"))
    private User reportingManager;

    @Column(name = "employment_type", nullable = false, length = 50)
    private String employmentType;

    @Column(name = "date_of_joining", nullable = false)
    private LocalDate dateOfJoining;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Current Address
    @Column(name = "current_address_line1", nullable = false, length = 255)
    private String currentAddressLine1;

    @Column(name = "current_address_line2", length = 255)
    private String currentAddressLine2;

    @Column(name = "current_city", nullable = false, length = 100)
    private String currentCity;

    @Column(name = "current_state", nullable = false, length = 100)
    private String currentState;

    @Column(name = "current_country", nullable = false, length = 100)
    private String currentCountry;

    @Column(name = "current_pincode", nullable = false, length = 20)
    private String currentPincode;

    // Permanent Address
    @Column(name = "permanent_address_line1", nullable = false, length = 255)
    private String permanentAddressLine1;

    @Column(name = "permanent_address_line2", length = 255)
    private String permanentAddressLine2;

    @Column(name = "permanent_city", nullable = false, length = 100)
    private String permanentCity;

    @Column(name = "permanent_state", nullable = false, length = 100)
    private String permanentState;

    @Column(name = "permanent_country", nullable = false, length = 100)
    private String permanentCountry;

    @Column(name = "permanent_pincode", nullable = false, length = 20)
    private String permanentPincode;

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
