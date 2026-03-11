package com.security.rbac.multitenancy.exception;

import com.security.rbac.exception.ApiBaseException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a request requires a tenant context (e.g., Tenant Login),
 * but no `X-Tenant-ID` header or subdomain was provided, resolving to public
 * schema incorrectly.
 */
public class TenantNotProvidedException extends ApiBaseException {
    public TenantNotProvidedException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
