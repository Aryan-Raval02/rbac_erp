package com.security.rbac.modules.user.service;

import com.security.rbac.modules.user.dto.request.CreateUserRequest;
import com.security.rbac.modules.user.dto.response.UserResponse;

/** Service interface for tenant-scoped user operations. */
public interface UserService {

    /**
     * Creates a new user in the active tenant schema.
     * Validates uniqueness of email and username within the tenant.
     */
    UserResponse createUser(CreateUserRequest request);
}
