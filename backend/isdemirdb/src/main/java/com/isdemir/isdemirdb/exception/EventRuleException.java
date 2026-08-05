package com.isdemir.isdemirdb.exception;

// Thrown from the service layer when an event business rule is violated.
// GlobalExceptionHandler turns it into 400 + ApiResponse{success:false}.
public class EventRuleException extends RuntimeException {

    public EventRuleException(String message) {
        super(message);
    }
}
