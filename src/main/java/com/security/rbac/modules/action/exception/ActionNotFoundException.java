package com.security.rbac.modules.action.exception;

import com.security.rbac.exception.ApiBaseException;
import org.springframework.http.HttpStatus;

public class ActionNotFoundException extends ApiBaseException {
    public ActionNotFoundException(String message){
        super(message, HttpStatus.NOT_FOUND);
    }
}
