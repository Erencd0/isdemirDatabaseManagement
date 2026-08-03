package com.isdemir.isdemirdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @JsonProperty("kullanici_adi")
    private String kullaniciAdi;

    @JsonProperty("kullanici_parola")
    private String kullaniciParola;
}
