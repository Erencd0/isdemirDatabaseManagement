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
@Table(name = "kullanici")
@Getter
@Setter
@NoArgsConstructor
public class Kullanici {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kullanici_id")
    private Integer kullaniciId;

    @Column(name = "kullanici_adi", unique = true)
    private String kullaniciAdi;

    @Column(name = "kullanici_parola")
    private String kullaniciParola;

    @Column(name = "rol_adi")
    private String rolAdi;
}
