package com.isdemir.isdemirdb.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

import com.isdemir.isdemirdb.dto.HeatDetailResponse;
import com.isdemir.isdemirdb.entity.Heat;
import com.isdemir.isdemirdb.entity.HeatPhoto;
import com.isdemir.isdemirdb.entity.MaterialUsage;
import com.isdemir.isdemirdb.service.HeatService;
import com.isdemir.isdemirdb.service.SupabaseStorage;

import lombok.RequiredArgsConstructor;

// The business logic lives in HeatService; exception -> HTTP status mapping is done in
// GlobalExceptionHandler. The URLs stay Turkish because they are the public API contract.
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HeatController {

    private final HeatService heatService;
    private final SupabaseStorage supabaseStorage;

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

    // Every heat of every converter, for the timeline page. Unlike /dokum it is NOT limited
    // to the user's own converters; the timeline compares the three converters against each
    // other. A valid token is still required. The literal path wins over /dokum/{id}.
    @GetMapping("/dokum/zaman-cizelgesi")
    public List<Heat> timeline() {
        return heatService.findAllConverters();
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

    // Adds a photo to a heat from the panel. multipart/form-data: "dosya" is the file itself,
    // "kullaniciId" is who uploaded it (the same field the material endpoints take).
    // Answers with the saved row, so the screen can show it without reloading the detail.
    @PostMapping("/dokum/{id}/fotograf")
    public HeatPhoto uploadPhoto(@PathVariable Integer id,
            @RequestParam("dosya") MultipartFile dosya,
            @RequestParam(value = "kullaniciId", required = false) Long kullaniciId) {
        return heatService.addPhoto(id, dosya, kullaniciId);
    }

    // One photo of a heat, as the image itself. The metadata (id, name, type) already comes
    // with the detail above; this endpoint only serves the bytes, pulled from the private
    // Supabase Storage bucket. It needs a token like every other /api path, so the photos
    // stay behind the login even though the frontend shows them in an <img>.
    @GetMapping("/dokum/{id}/fotograf/{fotografId}")
    public ResponseEntity<byte[]> photo(@PathVariable Integer id, @PathVariable Long fotografId) {
        HeatPhoto photo = heatService.requirePhoto(id, fotografId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .body(supabaseStorage.download(photo.getStoragePath()));
    }

    // Deletes a photo of a heat: the row and the file in the bucket both go, so it cannot be
    // undone - the frontend asks before calling this.
    @DeleteMapping("/dokum/{id}/fotograf/{fotografId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@PathVariable Integer id, @PathVariable Long fotografId) {
        heatService.deletePhoto(id, fotografId);
    }

    // The same detail, but by dokum_no. The "Yenile" button of the frontend uses this (see
    // the detailByNo service). The two segment path (/dokum/no/{heatNo}) does not collide
    // with the single segment /dokum/{id}.
    @GetMapping("/dokum/no/{heatNo}")
    public HeatDetailResponse detailByNo(@PathVariable Integer heatNo) {
        return heatService.detailByNo(heatNo);
    }
}
