package com.isdemir.isdemirdb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "malzeme_tablosu")
public class Malzeme {

    // malzeme_id DB'de sequence (identity) ile uretilir
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "malzeme_id")
    private Integer malzemeId;

    @Column(name = "malzeme_kodu")
    private Integer malzemeKodu;

    @Column(name = "malzeme_turu")
    private String malzemeTuru;

    @Column(name = "malzeme_adi")
    private String malzemeAdi;

    @Column(name = "aktif_pasif")
    private Boolean aktifPasif;

    public Malzeme() {
    }

    public Integer getMalzemeId() {
        return malzemeId;
    }

    public void setMalzemeId(Integer malzemeId) {
        this.malzemeId = malzemeId;
    }

    public Integer getMalzemeKodu() {
        return malzemeKodu;
    }

    public void setMalzemeKodu(Integer malzemeKodu) {
        this.malzemeKodu = malzemeKodu;
    }

    public String getMalzemeTuru() {
        return malzemeTuru;
    }

    public void setMalzemeTuru(String malzemeTuru) {
        this.malzemeTuru = malzemeTuru;
    }

    public String getMalzemeAdi() {
        return malzemeAdi;
    }

    public void setMalzemeAdi(String malzemeAdi) {
        this.malzemeAdi = malzemeAdi;
    }

    public Boolean getAktifPasif() {
        return aktifPasif;
    }

    public void setAktifPasif(Boolean aktifPasif) {
        this.aktifPasif = aktifPasif;
    }
}
