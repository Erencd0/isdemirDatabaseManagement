package com.isdemir.isdemirdb.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

// One result per item of a bulk material insert. Even when an item fails (success=false)
// the others are still saved, so the client can see which item was rejected and why.
@Getter
@AllArgsConstructor
public class MaterialResponse {

    private boolean success;
    private String message;
    private Integer heatId;
    private String additive;
    private Integer materialCode;
    private Integer quantity;
    private LocalDateTime addedTime;
    private Integer usageId;   // kullanim_id when saved; null when rejected
}

// TODO(cagri): a Materials class will be created
