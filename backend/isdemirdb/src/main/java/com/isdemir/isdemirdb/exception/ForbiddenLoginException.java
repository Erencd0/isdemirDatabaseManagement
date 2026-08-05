package com.isdemir.isdemirdb.exception;

// User was found but rol_adi is empty -> results in 403 (see GlobalExceptionHandler)
public class ForbiddenLoginException extends RuntimeException {

    public ForbiddenLoginException(String message) {
        super(message);
    }
}
