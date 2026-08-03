package com.isdemir.isdemirdb.exception;

// Istenen kayit (dokum, malzeme kullanimi vb.) bulunamadiginda firlatilir -> HTTP 404.
// HTTP kodu cevrimi GlobalExceptionHandler'da yapilir.
public class KayitBulunamadiException extends RuntimeException {
    public KayitBulunamadiException(String message) {
        super(message);
    }
}
