package com.isdemir.isdemirdb.exception;

// Gonderilen veri is kurallarina uymadiginda firlatilir -> HTTP 400.
// HTTP kodu cevrimi GlobalExceptionHandler'da yapilir.
public class GecersizVeriException extends RuntimeException {
    public GecersizVeriException(String message) {
        super(message);
    }
}
