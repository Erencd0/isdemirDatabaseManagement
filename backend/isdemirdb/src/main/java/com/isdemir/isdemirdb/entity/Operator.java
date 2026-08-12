package com.isdemir.isdemirdb.entity;

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

// Maps the operator_tablosu table: the people who run a heat. @JsonProperty keeps the JSON
// keys Turkish, like every other entity here.
//
// aktif = the operator still works here. Only aktif operators can be picked for a new heat;
// an inactive one stays on the heats they ran in the past.
@Entity
@Table(name = "operator_tablosu")
@Getter
@Setter
@NoArgsConstructor
public class Operator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "operator_adi")
    @JsonProperty("operatorAdi")
    private String name;

    @Column(name = "operator_soyadi")
    @JsonProperty("operatorSoyadi")
    private String surname;

    // Only "dokumcu" for now; other roles may follow.
    @Column(name = "operator_rolu")
    @JsonProperty("operatorRolu")
    private String role;

    @Column(name = "aktif")
    @JsonProperty("aktif")
    private Boolean active;
}
