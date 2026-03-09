package com.security.rbac.modules.ceo.exception;

import com.security.rbac.exception.ApiBaseException;
import org.springframework.http.HttpStatus;

public class CeoFoundException extends ApiBaseException {
    public CeoFoundException(String message) {
        super(message, HttpStatus.FOUND);
    }
}
