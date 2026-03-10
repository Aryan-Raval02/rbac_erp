package com.security.rbac.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user lacks the necessary permission or role
 * to access a specific resource.
 */
public class UnauthorizedException extends ApiBaseException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
