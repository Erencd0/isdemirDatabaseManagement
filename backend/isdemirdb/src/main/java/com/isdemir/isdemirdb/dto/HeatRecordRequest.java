package com.isdemir.isdemirdb.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

// The SINGLE body posted to /api/dokum/event: all data of one heat. heatId (= dokum_no)
// is given once at the top level; the event and material items below do not carry their
// own heatId, they use this top level value. Both lists are OPTIONAL -- either one may be
// missing/empty (e.g. only events or only materials can be sent).
// Example:
//   { "heatId": 6300001,
//     "events":    [ { "eventNo":1, "eventTime":"2026-08-04T08:15:00" } ],
//     "materials": [ { "additive":"KONVKATKI", "materialCode":12, "quantity":500,
//                      "addedTime":"2026-08-04T09:15:00" } ] }
@Getter
@Setter
public class HeatRecordRequest {

    private Integer heatId;                  // dokum_no (applies to every item)
    private List<EventRequest> events;       // optional
    private List<MaterialRequest> materials; // optional
}
