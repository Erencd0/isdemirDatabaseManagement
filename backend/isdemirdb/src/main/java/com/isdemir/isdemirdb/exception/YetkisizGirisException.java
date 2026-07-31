package com.isdemir.isdemirdb.exception;

// Kullanici bulundu ama rol_adi bos -> 403 ile sonuclanir (bkz. GlobalExceptionHandler)
public class YetkisizGirisException extends RuntimeException {

    public YetkisizGirisException(String mesaj) {
        super(mesaj);
    }
}
