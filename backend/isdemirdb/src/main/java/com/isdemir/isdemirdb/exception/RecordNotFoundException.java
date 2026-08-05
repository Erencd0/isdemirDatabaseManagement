package com.isdemir.isdemirdb.exception;

// Thrown when the requested record (heat, material usage etc.) does not exist -> HTTP 404.
// The HTTP status mapping happens in GlobalExceptionHandler.
public class RecordNotFoundException extends RuntimeException {
    public RecordNotFoundException(String message) {
        super(message);
    }
}
