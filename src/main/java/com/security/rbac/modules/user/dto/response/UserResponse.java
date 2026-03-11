package com.security.rbac.modules.user.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record UserResponse(
                Long id,
                String empId,
                String firstName,
                String lastName,
                String email,
                String username,
                String contactNumber,
                String alternateNumber,
                String department,
                String designation,
                Long roleId,
                String roleName,
                Long reportingManagerId,
                String employmentType,
                LocalDate dateOfJoining,
                String status,
                Boolean isActive,
                String currentAddressLine1,
                String currentAddressLine2,
                String currentCity,
                String currentState,
                String currentCountry,
                String currentPincode,
                String permanentAddressLine1,
                String permanentAddressLine2,
                String permanentCity,
                String permanentState,
                String permanentCountry,
                String permanentPincode,
                Instant createdAt) {
}
