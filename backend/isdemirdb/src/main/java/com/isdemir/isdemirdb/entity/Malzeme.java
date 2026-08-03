package com.isdemir.isdemirdb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "malzeme_tablosu")
@Getter
@Setter
@NoArgsConstructor
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
}
