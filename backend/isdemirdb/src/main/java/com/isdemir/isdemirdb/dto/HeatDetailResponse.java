package com.isdemir.isdemirdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.isdemir.isdemirdb.entity.Heat;
import com.isdemir.isdemirdb.entity.HeatPhoto;
import com.isdemir.isdemirdb.entity.MaterialUsage;
import com.isdemir.isdemirdb.entity.Operator;

import lombok.AllArgsConstructor;
import lombok.Getter;

// For "detay gor": the heat itself plus the materials added to it.
// @JsonProperty keeps the JSON keys the frontend already reads.
@Getter
@AllArgsConstructor
public class HeatDetailResponse {

    @JsonProperty("dokum")
    private Heat heat;

    @JsonProperty("malzemeler")
    private List<MaterialUsage> materials;

    // The operator who ran this heat, name included - null when the heat has none (every heat
    // recorded before operators existed). It carries "aktif" too, so the screen can warn that
    // this person does not work here any more.
    @JsonProperty("operator")
    private Operator operator;

    // The photos of this heat (uploaded from the mobile app), metadata only - empty list when
    // there are none. The image itself is downloaded from
    // /api/dokum/{id}/fotograf/{fotografId}, so the list stays small.
    @JsonProperty("fotograflar")
    private List<HeatPhoto> photos;
}
