package com.security.rbac.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when providing a bad username, email, or password during login.
 */
public class InvalidCredentialsException extends ApiBaseException {
    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
