package com.security.rbac.modules.ceo.controller;

import com.security.rbac.modules.ceo.dto.request.SignUpRequest;
import com.security.rbac.modules.ceo.dto.response.SignUpResponse;
import com.security.rbac.modules.ceo.service.GlobalUserService;
import com.security.rbac.utility.ResponseBuilder;
import com.security.rbac.utility.ResponseStructure;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles public sign-up for CEOs / company owners.
 *
 * <p>
 * Base path: {@code /api/v1/auth}
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class GlobalUserController {

    private final GlobalUserService signUpService;

    /**
     * Register a new company and its CEO user.
     *
     * <pre>
     * POST /api/v1/auth/signup
     * {
     *   "companyName" : "Acme Corp",
     *   "fullName"    : "John Doe",
     *   "email"       : "john@acme.com",
     *   "username"    : "john_doe",
     *   "password"    : "SecurePass123!",
     *   "phoneNumber" : "9876543210"    (optional)
     * }
     * </pre>
     *
     * <p>
     * Response {@code 201 Created} — includes tenant details + CEO user details.
     */
    @PostMapping("/signup")
    public ResponseEntity<ResponseStructure<SignUpResponse>> signUp(
            @Valid @RequestBody SignUpRequest request) {

        SignUpResponse response = signUpService.signUp(request);
        return ResponseBuilder.success(HttpStatus.OK, "Sign up successful!", response);
    }
}
