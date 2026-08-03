package com.isdemir.isdemirdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    @JsonProperty("kullanici_id")
    private Integer kullaniciId;

    @JsonProperty("kullanici_adi")
    private String kullaniciAdi;

    private List<String> roller;
}
