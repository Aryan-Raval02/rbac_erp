package com.security.rbac.modules.tenant.exception;

import com.security.rbac.exception.ApiBaseException;
import org.springframework.http.HttpStatus;

public class TenantNotFoundException extends ApiBaseException {
    public TenantNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
