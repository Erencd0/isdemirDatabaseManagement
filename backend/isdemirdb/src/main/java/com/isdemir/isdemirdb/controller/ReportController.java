package com.isdemir.isdemirdb.controller;

import java.io.IOException;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isdemir.isdemirdb.exception.InvalidDataException;
import com.isdemir.isdemirdb.service.ReportService;

import lombok.RequiredArgsConstructor;

// Rapor: the user picks a date interval on the dashboard and gets an .xlsx back.
// The interval check stays here (it is about the request); the query and the Excel itself
// are ReportService's job.
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReportService reportService;

    @GetMapping("/rapor")
    public ResponseEntity<byte[]> report(
            @RequestParam("baslangic") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("bitis") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end)
            throws IOException {

        if (end.isBefore(start)) {
            throw new InvalidDataException("Bitis tarihi baslangic tarihinden once olamaz.");
        }

        String fileName = "rapor-" + start + "_" + end + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType(XLSX))
                .body(reportService.build(start, end));
    }
}
