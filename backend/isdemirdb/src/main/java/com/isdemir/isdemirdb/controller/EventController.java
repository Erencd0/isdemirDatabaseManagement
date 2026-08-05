package com.isdemir.isdemirdb.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isdemir.isdemirdb.dto.ApiResponse;
import com.isdemir.isdemirdb.dto.HeatRecordRequest;
import com.isdemir.isdemirdb.dto.HeatRecordResponse;
import com.isdemir.isdemirdb.dto.MaterialResponse;
import com.isdemir.isdemirdb.service.HeatRecordService;

import lombok.RequiredArgsConstructor;

// The single data entry point of the automation. The events AND the materials of one heat
// are posted to the same URL in one body (see HeatRecordRequest). The business logic lives
// in HeatRecordService; exception -> ApiResponse mapping is done in GlobalExceptionHandler.
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174" })
@RequiredArgsConstructor
public class EventController {

    private final HeatRecordService heatRecordService;

    // heatId at the top level, events[] and materials[] below it (both optional):
    // { "heatId":6300001, "events":[...], "materials":[...] }
    @PostMapping("/dokum/event")
    public ApiResponse<HeatRecordResponse> save(@RequestBody HeatRecordRequest request) {
        HeatRecordResponse data = heatRecordService.save(request);

        long materialsSaved = data.getMaterials().stream()
                .filter(MaterialResponse::isSuccess).count();
        long materialsRejected = data.getMaterials().size() - materialsSaved;
        String message = data.getEvents().size() + " event, " + materialsSaved
                + " malzeme kaydedildi" + (materialsRejected > 0
                        ? " (" + materialsRejected + " malzeme reddedildi)." : ".");
        return ApiResponse.success(message, data);
    }
}
