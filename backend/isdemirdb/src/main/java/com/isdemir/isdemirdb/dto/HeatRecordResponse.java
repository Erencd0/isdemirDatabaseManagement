package com.isdemir.isdemirdb.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Response of /api/dokum/event: event results and material results in separate arrays.
// The side that was not submitted comes back as an empty array.
@Getter
@AllArgsConstructor
public class HeatRecordResponse {

    private List<EventResponse> events;
    private List<MaterialResponse> materials;
}
