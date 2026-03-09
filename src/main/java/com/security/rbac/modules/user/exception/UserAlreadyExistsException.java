package com.security.rbac.modules.user.exception;

import com.security.rbac.exception.ApiBaseException;
import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends ApiBaseException {

    public UserAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
