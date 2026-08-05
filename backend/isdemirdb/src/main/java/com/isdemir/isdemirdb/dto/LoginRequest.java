package com.isdemir.isdemirdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// POST /auth/login request body: { "kullanici_adi": "...", "kullanici_parola": "..." }
@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @JsonProperty("kullanici_adi")
    private String username;

    @JsonProperty("kullanici_parola")
    private String password;
}
