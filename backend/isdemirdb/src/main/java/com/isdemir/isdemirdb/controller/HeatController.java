package com.isdemir.isdemirdb.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.isdemir.isdemirdb.dto.HeatDetailResponse;
import com.isdemir.isdemirdb.entity.Heat;
import com.isdemir.isdemirdb.entity.MaterialUsage;
import com.isdemir.isdemirdb.service.HeatService;

import lombok.RequiredArgsConstructor;

// The business logic lives in HeatService; exception -> HTTP status mapping is done in
// GlobalExceptionHandler. The URLs stay Turkish because they are the public API contract.
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174" })
@RequiredArgsConstructor
public class HeatController {

    private final HeatService heatService;

    // Lists every heat
    @GetMapping("/dokum")
    public List<Heat> list() {
        return heatService.findAll();
    }

    // Adds a new heat
    @PostMapping("/dokum")
    public Heat create(@RequestBody Heat heat) {
        return heatService.create(heat);
    }

    // Returns the next heat number this converter will get, before saving.
    // The frontend uses it to show a read-only number on the form.
    @GetMapping("/dokum/sonraki-no")
    public Integer nextHeatNo(@RequestParam("konverterNo") Integer converterNo) {
        return heatService.nextHeatNo(converterNo);
    }

    // Adds a material to a heat.
    @PostMapping("/dokum/{id}/malzeme")
    public MaterialUsage addMaterial(@PathVariable Integer id,
            @RequestBody MaterialUsage materialUsage) {
        return heatService.addMaterial(id, materialUsage);
    }

    // Updates a material usage of a heat.
    @PutMapping("/dokum/{id}/malzeme/{usageId}")
    public MaterialUsage updateMaterial(@PathVariable Integer id,
            @PathVariable Integer usageId,
            @RequestBody MaterialUsage incoming) {
        return heatService.updateMaterial(id, usageId, incoming);
    }

    // Deletes a material usage of a heat.
    @DeleteMapping("/dokum/{id}/malzeme/{usageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMaterial(@PathVariable Integer id, @PathVariable Integer usageId) {
        heatService.deleteMaterial(id, usageId);
    }

    // "Detay gor": returns the heat together with every material added to it
    @GetMapping("/dokum/{id}")
    public HeatDetailResponse detail(@PathVariable Integer id) {
        return heatService.detail(id);
    }

    // The same detail, but by dokum_no. The "Yenile" button of the frontend uses this (see
    // the detailByNo service). The two segment path (/dokum/no/{heatNo}) does not collide
    // with the single segment /dokum/{id}.
    @GetMapping("/dokum/no/{heatNo}")
    public HeatDetailResponse detailByNo(@PathVariable Integer heatNo) {
        return heatService.detailByNo(heatNo);
    }
}
