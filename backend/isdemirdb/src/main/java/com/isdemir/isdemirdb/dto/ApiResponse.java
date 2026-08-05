package com.isdemir.isdemirdb.dto;

import lombok.Getter;

// Shared response wrapper of every event endpoint.
// Success: { "success": true,  "message": "...", "data": {...} }
// Failure: { "success": false, "message": "...", "data": null }
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Successful response, together with the payload
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    // Failed response: message only (data is null). Used by GlobalExceptionHandler.
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
