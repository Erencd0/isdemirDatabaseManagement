package com.isdemir.isdemirdb.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Maps the dokum_tablosu table. Column names stay Turkish (they are the real DB columns)
// and @JsonProperty keeps the JSON contract unchanged for the frontend/Postman.
@Entity
@Table(name = "dokum_tablosu")
@Getter
@Setter
@NoArgsConstructor
public class Heat {

    // The id is assigned manually instead of by a DB sequence (MAX(id)+1), so failed
    // POSTs do not advance a sequence and leave gaps in the numbering.
    @Id
    @Column(name = "dokum_id")
    @JsonProperty("dokumId")
    private Integer id;

    @Column(name = "dokum_no")
    @JsonProperty("dokumNo")
    private Integer heatNo;

    @Column(name = "konverter_no")
    @JsonProperty("konverterNo")
    private Integer converterNo;

    @Column(name = "hurda_sarj_baslama_zamani")
    @JsonProperty("hurdaSarjBaslamaZamani")
    private LocalDateTime scrapChargeStartTime;

    @Column(name = "hurda_sarj_bitis_zamani")
    @JsonProperty("hurdaSarjBitisZamani")
    private LocalDateTime scrapChargeEndTime;

    @Column(name = "ana_uflemeye_baslama_zamani")
    @JsonProperty("anaUflemeyeBaslamaZamani")
    private LocalDateTime mainBlowStartTime;

    @Column(name = "ana_ufleme_bitis_zamani")
    @JsonProperty("anaUflemeBitisZamani")
    private LocalDateTime mainBlowEndTime;

    @Column(name = "dokum_zamani")
    @JsonProperty("dokumZamani")
    private LocalDateTime tapTime;

    @Column(name = "shd_sicaklik")
    @JsonProperty("shdSicaklik")
    private Integer shdTemperature;

    @Column(name = "dokum_sicaklik")
    @JsonProperty("dokumSicaklik")
    private Integer tapTemperature;

    @Column(name = "lans_skal_durum")
    @JsonProperty("lansSkalDurum")
    private String lanceSkullStatus;

    @Column(name = "kayit_zamani")
    @JsonProperty("kayitZamani")
    private LocalDateTime createdAt;

    @Column(name = "kullanici_id")
    @JsonProperty("kullaniciId")
    private Integer userId;

    // The operator (dokumcu) who ran this heat. Null on the heats recorded before operators
    // existed. Only an aktif operator may be set on a new heat; an operator that later goes
    // inactive stays on the old heats they ran.
    @Column(name = "operator_id")
    @JsonProperty("operatorId")
    private Long operatorId;
}
