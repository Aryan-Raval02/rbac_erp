package com.security.rbac.modules.tenant.exception;

import com.security.rbac.exception.ApiBaseException;
import org.springframework.http.HttpStatus;

public class TenantFoundException extends ApiBaseException {
    public TenantFoundException(String message) {
        super(message, HttpStatus.FOUND);
    }
}
