package com.security.rbac.modules.module.exception;

import com.security.rbac.exception.ApiBaseException;
import org.springframework.http.HttpStatus;

public class ModuleNotFoundException extends ApiBaseException {
    public ModuleNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
