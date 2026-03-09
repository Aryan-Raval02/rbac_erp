package com.security.rbac.modules.role.exception;

import com.security.rbac.exception.ApiBaseException;
import org.springframework.http.HttpStatus;

public class RoleNotFoundException extends ApiBaseException {

    public RoleNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
