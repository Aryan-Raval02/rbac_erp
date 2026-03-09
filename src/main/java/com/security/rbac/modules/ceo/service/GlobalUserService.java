package com.security.rbac.modules.ceo.service;

import com.security.rbac.modules.ceo.dto.request.SignUpRequest;
import com.security.rbac.modules.ceo.dto.response.SignUpResponse;

/** Handles CEO / company sign-up flow. */
public interface GlobalUserService {

    /**
     * Registers a new company and its CEO.
     * <ol>
     * <li>Validates uniqueness of email, username, and company schema</li>
     * <li>Creates the tenant PostgreSQL schema + runs migrations</li>
     * <li>Persists the tenant in {@code public.tenant_registry}</li>
     * <li>Persists the CEO in {@code public.global_users}</li>
     * </ol>
     */
    SignUpResponse signUp(SignUpRequest request);
}
