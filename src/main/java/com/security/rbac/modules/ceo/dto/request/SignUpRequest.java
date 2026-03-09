package com.security.rbac.modules.ceo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for CEO / company sign-up.
 *
 * <pre>
 * POST /api/v1/auth/signup
 * {
 *   "companyName" : "Acme Corp",
 *   "fullName"    : "John Doe",
 *   "email"       : "john@acme.com",
 *   "username"    : "john_doe",
 *   "password"    : "SecurePass123!",
 *   "phoneNumber" : "9876543210"
 * }
 * </pre>
 *
 * <p>
 * The tenant {@code schemaName} is auto-derived from {@code companyName}.
 */
public record SignUpRequest(

        @NotBlank(message = "companyName is required") String companyName,

        @NotBlank(message = "fullName is required") String fullName,

        @NotBlank(message = "email is required") @Email(message = "email must be a valid email address") String email,

        @NotBlank(message = "username is required") @Size(min = 3, max = 50, message = "username must be 3–50 characters") String username,

        @NotBlank(message = "password is required") @Size(min = 8, message = "password must be at least 8 characters") String password,

        String phoneNumber // optional
) {
}
