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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Maps the dokum_fotograf table: the photos of a heat, uploaded by the mobile app.
// The row is metadata only - the file itself sits in the Supabase Storage bucket and
// dosya_yolu is its path there (see SupabaseStorage).
//
// The veri (bytea) column is deliberately NOT mapped: the mobile app leaves it null and
// mapping it would pull megabytes into every "detay gor" query.
@Entity
@Table(name = "dokum_fotograf")
@Getter
@Setter
@NoArgsConstructor
public class HeatPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fotograf_id")
    @JsonProperty("fotografId")
    private Long id;

    @Column(name = "dokum_id")
    @JsonProperty("dokumId")
    private Long heatId;

    @Column(name = "dosya_adi")
    @JsonProperty("dosyaAdi")
    private String fileName;

    @Column(name = "icerik_turu")
    @JsonProperty("icerikTuru")
    private String contentType;

    @Column(name = "boyut_bayt")
    @JsonProperty("boyutBayt")
    private Long sizeBytes;

    @Column(name = "yukleyen_kullanici_id")
    @JsonProperty("yukleyenKullaniciId")
    private Long uploadedByUserId;

    @Column(name = "yuklenme_zamani")
    @JsonProperty("yuklenmeZamani")
    private LocalDateTime uploadedAt;

    // The path inside the storage bucket (e.g. "dokum/16/950e2678-....jpg"). Internal:
    // the browser cannot use it directly (the bucket is private), it downloads the bytes
    // through /api/dokum/{id}/fotograf/{fotografId} instead.
    @Column(name = "dosya_yolu")
    @JsonIgnore
    private String storagePath;
}
