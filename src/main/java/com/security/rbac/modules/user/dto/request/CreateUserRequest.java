package com.security.rbac.modules.user.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateUserRequest(

                @NotBlank(message = "empId is required") String empId,
                @NotBlank(message = "firstName is required") String firstName,
                @NotBlank(message = "lastName is required") String lastName,

                @NotBlank(message = "email is required") @Email(message = "email must be a valid email address") String email,
                @NotBlank(message = "username is required") @Size(min = 3, max = 50, message = "username must be 3–50 characters") String username,
                @NotBlank(message = "password is required") @Size(min = 8, message = "password must be at least 8 characters") String password,

                @NotBlank(message = "contactNumber is required") String contactNumber,
                String alternateNumber,

                @NotBlank(message = "department is required") String department,
                @NotBlank(message = "designation is required") String designation,
                @NotNull(message = "roleId is required") Long roleId,

                Long reportingManagerId,

                @NotBlank(message = "employmentType is required") String employmentType,
                @NotNull(message = "dateOfJoining is required") LocalDate dateOfJoining,

                String status,

                @NotBlank(message = "currentAddressLine1 is required") String currentAddressLine1,
                String currentAddressLine2,
                @NotBlank(message = "currentCity is required") String currentCity,
                @NotBlank(message = "currentState is required") String currentState,
                @NotBlank(message = "currentCountry is required") String currentCountry,
                @NotBlank(message = "currentPincode is required") String currentPincode,

                @NotBlank(message = "permanentAddressLine1 is required") String permanentAddressLine1,
                String permanentAddressLine2,
                @NotBlank(message = "permanentCity is required") String permanentCity,
                @NotBlank(message = "permanentState is required") String permanentState,
                @NotBlank(message = "permanentCountry is required") String permanentCountry,
                @NotBlank(message = "permanentPincode is required") String permanentPincode,

                @Valid List<PermissionRequest> permissions

) {
        public record PermissionRequest(
                        @NotNull(message = "moduleId is required") Long moduleId,
                        @NotNull(message = "actionId is required") Long actionId) {
        }
}
