package com.isdemir.isdemirdb.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.isdemir.isdemirdb.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

// Translates the exceptions thrown by the service layer into HTTP status codes.
// This is a web layer concern, kept out of the controllers and services.
// Every handler logs first: this is the single place where a request turns into an error,
// so the log shows why Postman got that status.
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("401 Unauthorized: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler(ForbiddenLoginException.class)
    public ResponseEntity<String> handleForbiddenLogin(ForbiddenLoginException ex) {
        log.warn("403 Forbidden (login): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    // ConverterAccess.require(): the token holds no role for that converter -> 403.
    // Wrapped in ApiResponse because the event endpoint (the main write path) reads that shape.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("403 Forbidden (yetki): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<String> handleInvalidData(InvalidDataException ex) {
        log.warn("400 Bad Request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    // The upload limit (spring.servlet.multipart.max-file-size) is a rule too, not a crash:
    // without this a too big photo would come back as a 500.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        log.warn("400 Bad Request (dosya cok buyuk): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Fotoğraf çok büyük (en fazla 15 MB)");
    }

    @ExceptionHandler(RecordNotFoundException.class)
    public ResponseEntity<String> handleRecordNotFound(RecordNotFoundException ex) {
        log.warn("404 Not Found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // Event business rules: 400 + the shared ApiResponse{success:false, message}.
    // Unlike the plain String body of the other endpoints, this one is wrapped.
    @ExceptionHandler(EventRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handleEventRule(EventRuleException ex) {
        log.warn("400 Bad Request (event kurali): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }
}
