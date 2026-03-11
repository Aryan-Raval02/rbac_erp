package com.security.rbac.jwt.exception;

import com.security.rbac.exception.ApiBaseException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an authentication attempt is made for a user account
 * that exists but is marked as inactive or disabled.
 */
public class InactiveUserException extends ApiBaseException {
    public InactiveUserException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
