package com.security.rbac.modules.userPermission.exception;

import com.security.rbac.exception.ApiBaseException;
import org.springframework.http.HttpStatus;

public class UserPermissionAlreadyExistsException extends ApiBaseException {

    public UserPermissionAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
