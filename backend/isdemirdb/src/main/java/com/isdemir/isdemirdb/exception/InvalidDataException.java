package com.isdemir.isdemirdb.exception;

// Thrown when the submitted data breaks a business rule -> HTTP 400.
// The HTTP status mapping happens in GlobalExceptionHandler.
public class InvalidDataException extends RuntimeException {
    public InvalidDataException(String message) {
        super(message);
    }
}
