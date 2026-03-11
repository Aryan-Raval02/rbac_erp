package com.security.rbac.multitenancy.exception;

import com.security.rbac.exception.ApiBaseException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a user authenticates with a JWT token belonging to one tenant,
 * but attempts to access a resource in a different tenant schema explicitly.
 */
public class TenantMismatchException extends ApiBaseException {
    public TenantMismatchException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
