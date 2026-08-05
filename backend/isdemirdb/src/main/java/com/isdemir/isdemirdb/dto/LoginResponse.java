package com.isdemir.isdemirdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

// POST /auth/login response. The JSON keys stay as the frontend already reads them.
@Getter
@AllArgsConstructor
public class LoginResponse {

    @JsonProperty("kullanici_id")
    private Integer userId;

    @JsonProperty("kullanici_adi")
    private String username;

    @JsonProperty("roller")
    private List<String> roles;

    // Stage 5: the JWT access token produced on a successful login.
    private String accessToken;

    // Stage 7: the opaque refresh token stored in the DB (used to get a new access token).
    private String refreshToken;
}
