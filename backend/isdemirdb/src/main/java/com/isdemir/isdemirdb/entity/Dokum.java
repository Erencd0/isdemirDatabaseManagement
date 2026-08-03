package com.isdemir.isdemirdb.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dokum_tablosu")
@Getter
@Setter
@NoArgsConstructor
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
}
