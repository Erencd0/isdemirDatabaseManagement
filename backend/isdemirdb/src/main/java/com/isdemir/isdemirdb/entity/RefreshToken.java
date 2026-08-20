package com.isdemir.isdemirdb.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Maps the refresh_tokens table created in stage 2.
// Holds a user's refresh token (an opaque UUID) and its validity information.
// FK: kullanici_id -> kullanici.kullanici_id
@Entity
@Table(name = "refresh_token_tablosu")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_id")
    private Long id;

    @Column(name = "kullanici_id")
    private Integer userId;

    @Column(name = "refresh_token", unique = true)
    private String token;

    @Column(name = "bitis_zamani")
    private LocalDateTime expiresAt;

    @Column(name = "olusturulma_zamani")
    private LocalDateTime createdAt;

    @Column(name = "aktif_pasif")
    private Boolean active;
}
