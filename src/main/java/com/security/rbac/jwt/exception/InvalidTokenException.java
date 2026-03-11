package com.security.rbac.jwt.exception;

import com.security.rbac.exception.ApiBaseException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a JWT token is expired, malformed, or otherwise invalid.
 */
public class InvalidTokenException extends ApiBaseException {
    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
