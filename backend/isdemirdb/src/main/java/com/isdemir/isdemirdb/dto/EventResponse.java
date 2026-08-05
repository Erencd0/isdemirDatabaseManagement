package com.isdemir.isdemirdb.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

// One element of the "events" array inside HeatRecordResponse: the result of one saved
// event. EventService produces one per successful event.
//   - heatId    = dokum_no
//   - eventNo   = which step (1..5)
//   - eventName = the readable name of eventNo (JSON key stays "eventAdi")
//   - eventTime = the value written into the matching time column
@Getter
@AllArgsConstructor
public class EventResponse {
    private Integer heatId;
    private Integer eventNo;

    @JsonProperty("eventAdi")
    private String eventName;

    private LocalDateTime eventTime;
}
