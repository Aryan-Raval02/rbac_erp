package com.security.rbac.modules.user.controller;

import com.security.rbac.modules.user.dto.request.CreateUserRequest;
import com.security.rbac.modules.user.dto.response.UserResponse;
import com.security.rbac.modules.user.service.UserService;
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
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for tenant-scoped user management.
 *
 * <p>
 * Every request to this controller must include the {@code X-Tenant-ID}
 * header so {@code TenantResolverFilter} routes DB operations to the correct
 * tenant schema.
 *
 * <p>
 * Base path: {@code /api/v1/users}
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Create a new user in the current tenant.
     *
     * <pre>
     * POST /api/v1/users
     * X-Tenant-ID: acme_corp
     * {
     * "fullName": "Jane Smith",
     * "email": "jane@acme.com",
     * "username": "jane_smith",
     * "password": "Jane@2024!",
     * "phoneNumber": "9876543210",
     * "roleId": 2,
     * "permissions": [
     *     {
     *         "moduleId": 1,
     *         "actionId": 2
     *     },
     *     {
     *         "moduleId": 1,
     *         "actionId": 1
     *     }
     * ]
     * }
     * </pre>
     *
     * <p>
     * Response {@code 201 Created} — includes user details (password excluded).
     */
    @PostMapping
    @PreAuthorize("hasAuthority('USER_ADD') or hasRole('CEO')")
    public ResponseEntity<ResponseStructure<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        UserResponse response = userService.createUser(request);
        return ResponseBuilder.success(HttpStatus.OK, "User created successfully!", response);
    }
}
