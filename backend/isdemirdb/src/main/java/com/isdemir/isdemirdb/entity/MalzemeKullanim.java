package com.isdemir.isdemirdb.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "malzeme_kullanim_alani")
public class MalzemeKullanim {

    // kullanim_id DB'de "generated always as identity" ile uretilir,
    // bu yuzden elle set edilmez; IDENTITY strategisi DB'ye birakir.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kullanim_id")
    private Integer kullanimId;

    @Column(name = "dokum_id")
    private Integer dokumId;

    @Column(name = "malzeme_kodu")
    private Integer malzemeKodu;

    @Column(name = "miktar")
    private Integer miktar;

    @Column(name = "malzeme_verilis_tarihi")
    private LocalDateTime malzemeVerilisTarihi;

    @Column(name = "kullanici_id")
    private Integer kullaniciId;

    // islem_zamani DB'de LOCALTIMESTAMP default'u ile otomatik dolar.
    // insertable=false diyerek INSERT'e dahil etmiyoruz, degeri DB atiyor.
    @Column(name = "islem_zamani", insertable = false, updatable = false)
    private LocalDateTime islemZamani;

    // DB'de kolon degil; "detay gor"da malzeme_kodu'na karsilik gelen ad buraya doldurulur.
    @Transient
    private String malzemeAdi;

    // DB'de kolon degil; "detay gor"da malzeme_kodu'na karsilik gelen tur (KONVKATKI vb.)
    @Transient
    private String malzemeTuru;

    public MalzemeKullanim() {
    }

    public Integer getKullanimId() {
        return kullanimId;
    }

    public void setKullanimId(Integer kullanimId) {
        this.kullanimId = kullanimId;
    }

    public Integer getDokumId() {
        return dokumId;
    }

    public void setDokumId(Integer dokumId) {
        this.dokumId = dokumId;
    }

    public Integer getMalzemeKodu() {
        return malzemeKodu;
    }

    public void setMalzemeKodu(Integer malzemeKodu) {
        this.malzemeKodu = malzemeKodu;
    }

    public Integer getMiktar() {
        return miktar;
    }

    public void setMiktar(Integer miktar) {
        this.miktar = miktar;
    }

    public LocalDateTime getMalzemeVerilisTarihi() {
        return malzemeVerilisTarihi;
    }

    public void setMalzemeVerilisTarihi(LocalDateTime malzemeVerilisTarihi) {
        this.malzemeVerilisTarihi = malzemeVerilisTarihi;
    }

    public Integer getKullaniciId() {
        return kullaniciId;
    }

    public void setKullaniciId(Integer kullaniciId) {
        this.kullaniciId = kullaniciId;
    }

    public LocalDateTime getIslemZamani() {
        return islemZamani;
    }

    public void setIslemZamani(LocalDateTime islemZamani) {
        this.islemZamani = islemZamani;
    }

    public String getMalzemeAdi() {
        return malzemeAdi;
    }

    public void setMalzemeAdi(String malzemeAdi) {
        this.malzemeAdi = malzemeAdi;
    }

    public String getMalzemeTuru() {
        return malzemeTuru;
    }

    public void setMalzemeTuru(String malzemeTuru) {
        this.malzemeTuru = malzemeTuru;
    }
}
