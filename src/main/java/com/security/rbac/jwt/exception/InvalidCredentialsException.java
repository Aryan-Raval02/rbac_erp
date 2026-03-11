package com.security.rbac.jwt.exception;

import com.security.rbac.exception.ApiBaseException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when providing a bad username, email, or password during login.
 */
public class InvalidCredentialsException extends ApiBaseException {
    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
