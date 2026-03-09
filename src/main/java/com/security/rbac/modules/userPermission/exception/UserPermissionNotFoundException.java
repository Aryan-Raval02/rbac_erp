package com.security.rbac.modules.userPermission.exception;

import com.security.rbac.exception.ApiBaseException;
import org.springframework.http.HttpStatus;

public class UserPermissionNotFoundException extends ApiBaseException {

    public UserPermissionNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
