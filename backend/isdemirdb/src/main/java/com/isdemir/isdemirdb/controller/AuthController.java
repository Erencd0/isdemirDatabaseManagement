package com.isdemir.isdemirdb.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isdemir.isdemirdb.dto.ApiResponse;
import com.isdemir.isdemirdb.dto.LoginRequest;
import com.isdemir.isdemirdb.dto.LoginResponse;
import com.isdemir.isdemirdb.dto.RefreshRequest;
import com.isdemir.isdemirdb.dto.RefreshResponse;
import com.isdemir.isdemirdb.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174" })
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Stage 4: POST /auth/login -> find the user, check the password, allow the login.
    // The business logic lives in AuthService; exception -> HTTP status mapping is done in
    // GlobalExceptionHandler.
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // Stage 8: POST /auth/refresh -> produce a new access token from a valid refresh token.
    // Missing / inactive / expired refresh token -> 401 (GlobalExceptionHandler).
    @PostMapping("/refresh")
    public RefreshResponse refresh(@RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    // Stage 9: POST /auth/logout -> revoke the refresh token (aktif=false).
    // Token not present in the DB -> 401 (GlobalExceptionHandler).
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody RefreshRequest request) {
        authService.logout(request);
        return ApiResponse.success("Çıkış yapıldı. Refresh token iptal edildi.", null);
    }
}
