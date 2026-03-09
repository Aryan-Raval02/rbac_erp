package com.security.rbac.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiBaseException extends RuntimeException{
    private final HttpStatus status;

    public ApiBaseException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
