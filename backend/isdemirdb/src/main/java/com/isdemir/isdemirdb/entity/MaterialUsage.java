package com.isdemir.isdemirdb.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Maps the malzeme_kullanim_alani table: one material added to one heat.
// @JsonProperty keeps the JSON field names unchanged.
@Entity
@Table(name = "malzeme_kullanim")
@Getter
@Setter
@NoArgsConstructor
public class MaterialUsage {

    // kullanim_id is "generated always as identity" in the DB, so it is never set by
    // hand; the IDENTITY strategy leaves it to the DB.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "malzeme_kullanim_id")
    @JsonProperty("kullanimId")
    private Integer id;

    // FK to Heat.id (dokum_id) -- NOT the heat number. Careful: "heatId" in the
    // automation DTOs (EventRequest/MaterialRequest) means dokum_no instead.
    @Column(name = "dokum_id")
    @JsonProperty("dokumId")
    private Integer heatId;

    // FK to Material.id (malzeme_id) -- NOT the material code. The malzemeKodu the client
    // sends is translated into this id on write (HeatService.requireMaterialId).
    @Column(name = "malzeme_id")
    @JsonIgnore
    private Integer materialId;

    @Column(name = "miktar")
    @JsonProperty("miktar")
    private Double quantity;   // miktar DB'de double precision

    @Column(name = "malzeme_verilis_tarihi")
    @JsonProperty("malzemeVerilisTarihi")
    private LocalDateTime deliveredAt;

    @Column(name = "kullanici_id")
    @JsonProperty("kullaniciId")
    private Integer userId;

    // islem_zamani is filled automatically by the LOCALTIMESTAMP default in the DB.
    // insertable=false keeps it out of the INSERT so the DB assigns the value.
    @Column(name = "islem_zamani", insertable = false, updatable = false)
    @JsonProperty("islemZamani")
    private LocalDateTime processedAt;

    // Not a DB column; the malzeme_kodu of malzeme_id, filled on "detay gor" (view detail).
    @Transient
    @JsonProperty("malzemeKodu")
    private Integer materialCode;

    // Not a DB column; filled from malzeme_id on "detay gor" (view detail).
    @Transient
    @JsonProperty("malzemeAdi")
    private String materialName;

    // Not a DB column; the additive group of malzeme_kodu (KONVKATKI etc.) on "detay gor".
    @Transient
    @JsonProperty("malzemeTuru")
    private String materialType;
}
