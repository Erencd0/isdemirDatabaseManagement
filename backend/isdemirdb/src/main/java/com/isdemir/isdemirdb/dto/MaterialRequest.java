package com.isdemir.isdemirdb.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

// One element of the "materials" array inside HeatRecordRequest. It does NOT carry heatId;
// dokum_no comes from the top level (HeatRecordRequest.heatId) and applies to every material.
//   - additive     = malzeme_turu; materialCode must be an ACTIVE material of that type
//   - materialCode = malzeme_kodu
//   - quantity     = miktar
//   - addedTime    = malzeme_verilis_tarihi
// Example: { "additive":"KONVKATKI", "materialCode":12, "quantity":500,
//            "addedTime":"2026-08-04T09:15:00" }
@Getter
@Setter
public class MaterialRequest {

    private String additive;          // malzeme_turu
    private Integer materialCode;     // malzeme_kodu
    private Integer quantity;         // miktar
    private LocalDateTime addedTime;  // malzeme_verilis_tarihi
    private Integer userId;           // optional: kullanici_id
}
