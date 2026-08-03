package com.isdemir.isdemirdb.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isdemir.isdemirdb.entity.Malzeme;
import com.isdemir.isdemirdb.service.MalzemeService;

// Is mantigi MalzemeService'te; controller sadece bu servisi cagirir.
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174" })
public class MalzemeController {

    private final MalzemeService malzemeService;

    public MalzemeController(MalzemeService malzemeService) {
        this.malzemeService = malzemeService;
    }

    // Benzersiz malzeme turlerini listeler (combobox icin)
    @GetMapping("/malzeme/turler")
    public List<String> turler() {
        return malzemeService.turler();
    }

    // Secilen ture ait aktif malzemeleri listeler
    @GetMapping("/malzeme")
    public List<Malzeme> listele(@RequestParam String tur) {
        return malzemeService.tureGoreListele(tur);
    }
}
