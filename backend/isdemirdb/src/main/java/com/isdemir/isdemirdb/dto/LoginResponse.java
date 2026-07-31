package com.isdemir.isdemirdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginResponse {

    @JsonProperty("kullanici_id")
    private Integer kullaniciId;

    @JsonProperty("kullanici_adi")
    private String kullaniciAdi;

    private List<String> roller;

    public LoginResponse(Integer kullaniciId, String kullaniciAdi, List<String> roller) {
        this.kullaniciId = kullaniciId;
        this.kullaniciAdi = kullaniciAdi;
        this.roller = roller;
    }

    public Integer getKullaniciId() {
        return kullaniciId;
    }

    public String getKullaniciAdi() {
        return kullaniciAdi;
    }

    public List<String> getRoller() {
        return roller;
    }
}
