package com.isdemir.isdemirdb.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

// One element of the "events" array inside HeatRecordRequest. heatId is NOT carried in the
// body; dokum_no comes from the top level (HeatRecordRequest.heatId) and EventService sets
// it on every event.
// Example (array element): { "eventNo": 1, "eventTime": "2026-08-04T08:15:00" }
@Getter
@Setter
public class EventRequest {
    private Integer heatId;   // not sent in the body; EventService fills it from the top level
    private Integer eventNo;
    private LocalDateTime eventTime;
}
