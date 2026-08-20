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

// Maps the kullanici table. Never serialized to JSON directly (see LoginResponse).
@Entity
@Table(name = "\"Kullanici\"")   // buyuk harf -> tirnakli
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "kullanici_adi", unique = true)
    private String username;

    @Column(name = "parola")
    private String password;

    // Comma separated role list, e.g. "kv1,kv3" (see AuthService.parseRoles)
    @Column(name = "rol")
    private String roleName;
}
