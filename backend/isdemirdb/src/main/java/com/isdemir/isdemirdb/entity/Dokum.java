package com.isdemir.isdemirdb.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dokum_tablosu")
public class Dokum {

    // ID DB sequence'i yerine elle atanir (MAX(id)+1), boylece basarisiz
    // POST'lar sequence'i ilerletip numara atlamasina neden olmaz.
    @Id
    @Column(name = "dokum_id")
    private Integer dokumId;

    @Column(name = "dokum_no")
    private Integer dokumNo;

    @Column(name = "konverter_no")
    private Integer konverterNo;

    @Column(name = "hurda_sarj_baslama_zamani")
    private LocalDateTime hurdaSarjBaslamaZamani;

    @Column(name = "hurda_sarj_bitis_zamani")
    private LocalDateTime hurdaSarjBitisZamani;

    @Column(name = "ana_uflemeye_baslama_zamani")
    private LocalDateTime anaUflemeyeBaslamaZamani;

    @Column(name = "ana_ufleme_bitis_zamani")
    private LocalDateTime anaUflemeBitisZamani;

    @Column(name = "dokum_zamani")
    private LocalDateTime dokumZamani;

    @Column(name = "shd_sicaklik")
    private Integer shdSicaklik;

    @Column(name = "dokum_sicaklik")
    private Integer dokumSicaklik;

    @Column(name = "lans_skal_durum")
    private String lansSkalDurum;

    @Column(name = "kayit_zamani")
    private LocalDateTime kayitZamani;

    @Column(name = "kullanici_id")
    private Integer kullaniciId;

    public Dokum() {
    }

    public Integer getDokumId() {
        return dokumId;
    }

    public void setDokumId(Integer dokumId) {
        this.dokumId = dokumId;
    }

    public Integer getDokumNo() {
        return dokumNo;
    }

    public void setDokumNo(Integer dokumNo) {
        this.dokumNo = dokumNo;
    }

    public Integer getKonverterNo() {
        return konverterNo;
    }

    public void setKonverterNo(Integer konverterNo) {
        this.konverterNo = konverterNo;
    }

    public LocalDateTime getHurdaSarjBaslamaZamani() {
        return hurdaSarjBaslamaZamani;
    }

    public void setHurdaSarjBaslamaZamani(LocalDateTime hurdaSarjBaslamaZamani) {
        this.hurdaSarjBaslamaZamani = hurdaSarjBaslamaZamani;
    }

    public LocalDateTime getHurdaSarjBitisZamani() {
        return hurdaSarjBitisZamani;
    }

    public void setHurdaSarjBitisZamani(LocalDateTime hurdaSarjBitisZamani) {
        this.hurdaSarjBitisZamani = hurdaSarjBitisZamani;
    }

    public LocalDateTime getAnaUflemeyeBaslamaZamani() {
        return anaUflemeyeBaslamaZamani;
    }

    public void setAnaUflemeyeBaslamaZamani(LocalDateTime anaUflemeyeBaslamaZamani) {
        this.anaUflemeyeBaslamaZamani = anaUflemeyeBaslamaZamani;
    }

    public LocalDateTime getAnaUflemeBitisZamani() {
        return anaUflemeBitisZamani;
    }

    public void setAnaUflemeBitisZamani(LocalDateTime anaUflemeBitisZamani) {
        this.anaUflemeBitisZamani = anaUflemeBitisZamani;
    }

    public LocalDateTime getDokumZamani() {
        return dokumZamani;
    }

    public void setDokumZamani(LocalDateTime dokumZamani) {
        this.dokumZamani = dokumZamani;
    }

    public Integer getShdSicaklik() {
        return shdSicaklik;
    }

    public void setShdSicaklik(Integer shdSicaklik) {
        this.shdSicaklik = shdSicaklik;
    }

    public Integer getDokumSicaklik() {
        return dokumSicaklik;
    }

    public void setDokumSicaklik(Integer dokumSicaklik) {
        this.dokumSicaklik = dokumSicaklik;
    }

    public String getLansSkalDurum() {
        return lansSkalDurum;
    }

    public void setLansSkalDurum(String lansSkalDurum) {
        this.lansSkalDurum = lansSkalDurum;
    }

    public LocalDateTime getKayitZamani() {
        return kayitZamani;
    }

    public void setKayitZamani(LocalDateTime kayitZamani) {
        this.kayitZamani = kayitZamani;
    }

    public Integer getKullaniciId() {
        return kullaniciId;
    }

    public void setKullaniciId(Integer kullaniciId) {
        this.kullaniciId = kullaniciId;
    }
}
