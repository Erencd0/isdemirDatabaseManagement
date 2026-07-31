package com.isdemir.isdemirdb.exception;

// Kullanici adi / parola eslesmedi -> 401 ile sonuclanir (bkz. GlobalExceptionHandler)
public class GecersizKimlikException extends RuntimeException {

    public GecersizKimlikException(String mesaj) {
        super(mesaj);
    }
}
