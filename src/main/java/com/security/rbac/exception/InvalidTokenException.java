package com.security.rbac.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a JWT token is expired, malformed, or otherwise invalid.
 */
public class InvalidTokenException extends ApiBaseException {
    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
