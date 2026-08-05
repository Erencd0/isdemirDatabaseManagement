package com.isdemir.isdemirdb.exception;

// Username / password did not match -> results in 401 (see GlobalExceptionHandler)
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
