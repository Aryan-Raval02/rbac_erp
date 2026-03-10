package com.security.rbac.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Identifier (email or username) cannot be blank") String identifier,

        @NotBlank(message = "Password cannot be blank") String password) {
}
