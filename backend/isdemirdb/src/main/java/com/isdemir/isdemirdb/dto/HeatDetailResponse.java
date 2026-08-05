package com.isdemir.isdemirdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.isdemir.isdemirdb.entity.Heat;
import com.isdemir.isdemirdb.entity.MaterialUsage;

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
}
