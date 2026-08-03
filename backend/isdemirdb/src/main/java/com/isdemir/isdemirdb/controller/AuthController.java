package com.isdemir.isdemirdb.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isdemir.isdemirdb.dto.LoginRequest;
import com.isdemir.isdemirdb.dto.LoginResponse;
import com.isdemir.isdemirdb.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174" })
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Is mantigi AuthService'te; hata -> HTTP kodu cevrimi GlobalExceptionHandler'da.
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
