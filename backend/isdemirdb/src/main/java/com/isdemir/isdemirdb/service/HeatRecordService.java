package com.isdemir.isdemirdb.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.isdemir.isdemirdb.dto.EventResponse;
import com.isdemir.isdemirdb.dto.HeatRecordRequest;
import com.isdemir.isdemirdb.dto.HeatRecordResponse;
import com.isdemir.isdemirdb.dto.MaterialResponse;
import com.isdemir.isdemirdb.security.ConverterAccess;

import lombok.RequiredArgsConstructor;

// Coordinator of /api/dokum/event: handles both the time events and the materials of one
// heat in a single request. heatId (dokum_no) is given once at the top level and applies to
// both sides. events or materials may be empty/null; one side being empty still lets the
// other side be processed.
//
// The two sides have DIFFERENT rule regimes (on purpose):
//  - EVENTS run first, inside a SINGLE TRANSACTION. When one event breaks a rule an
//    EventRuleException is thrown -> 400 is returned and the materials are NEVER reached
//    (the events are not written either).
//  - MATERIALS run afterwards with PARTIAL SUCCESS. An item breaking a rule is rejected
//    while the others are still saved; the result carries success/message per item.
@Service
@RequiredArgsConstructor
public class HeatRecordService {

    private final EventService eventService;
    private final MaterialRecordService materialRecordService;
    private final ConverterAccess converterAccess;

    public HeatRecordResponse save(HeatRecordRequest request) {
        Integer heatId = request.getHeatId();
        // heatId (dokum_no) carries the converter in its 2nd digit, so the whole request
        // belongs to ONE converter: checked once here, before anything is written. A missing
        // heatId is left to the existing per-side validation.
        if (heatId != null) {
            converterAccess.require(ConverterAccess.converterOfHeatNo(heatId));
        }
        List<EventResponse> events = eventService.saveBatch(heatId, request.getEvents());
        List<MaterialResponse> materials = materialRecordService.saveBatch(heatId, request.getMaterials());
        return new HeatRecordResponse(events, materials);
    }
}
