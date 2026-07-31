package com.isdemir.isdemirdb.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Servis katmanindan firlatilan hatalari HTTP durum kodlarina cevirir.
// Web katmanina ait bu sorumluluk controller/servisten uzak tutulur.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GecersizKimlikException.class)
    public ResponseEntity<String> handleGecersizKimlik(GecersizKimlikException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler(YetkisizGirisException.class)
    public ResponseEntity<String> handleYetkisizGiris(YetkisizGirisException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}
