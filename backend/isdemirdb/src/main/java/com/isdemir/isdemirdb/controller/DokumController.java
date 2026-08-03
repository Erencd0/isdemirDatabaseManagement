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

import com.isdemir.isdemirdb.dto.DokumDetayResponse;
import com.isdemir.isdemirdb.entity.Dokum;
import com.isdemir.isdemirdb.entity.MalzemeKullanim;
import com.isdemir.isdemirdb.service.DokumService;

import lombok.RequiredArgsConstructor;

// Is mantigi DokumService'te; hata -> HTTP kodu cevrimi GlobalExceptionHandler'da.
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174" })
@RequiredArgsConstructor
public class DokumController {

    private final DokumService dokumService;

    // Tum dokumleri listeler
    @GetMapping("/dokum")
    public List<Dokum> listele() {
        return dokumService.tumDokumler();
    }

    // Yeni dokum ekler
    @PostMapping("/dokum")
    public Dokum ekle(@RequestBody Dokum dokum) {
        return dokumService.ekle(dokum);
    }

    // Kaydetmeden once, bu konverterin alacagi bir sonraki dokum_no'yu doner.
    // Frontend formda "degistirilemez" sekilde gostermek icin kullanir.
    @GetMapping("/dokum/sonraki-no")
    public Integer sonrakiNo(@RequestParam Integer konverterNo) {
        return dokumService.sonrakiDokumNo(konverterNo);
    }

    // Bir dokume malzeme ekler.
    @PostMapping("/dokum/{id}/malzeme")
    public MalzemeKullanim malzemeEkle(@PathVariable Integer id,
            @RequestBody MalzemeKullanim malzemeKullanim) {
        return dokumService.malzemeEkle(id, malzemeKullanim);
    }

    // Bir dokumdeki malzeme kullanimini gunceller.
    @PutMapping("/dokum/{id}/malzeme/{kullanimId}")
    public MalzemeKullanim malzemeGuncelle(@PathVariable Integer id,
            @PathVariable Integer kullanimId,
            @RequestBody MalzemeKullanim gelen) {
        return dokumService.malzemeGuncelle(id, kullanimId, gelen);
    }

    // Bir dokumdeki malzeme kullanimini siler.
    @DeleteMapping("/dokum/{id}/malzeme/{kullanimId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void malzemeSil(@PathVariable Integer id, @PathVariable Integer kullanimId) {
        dokumService.malzemeSil(id, kullanimId);
    }

    // "Detay gor": dokumu, o dokume eklenmis tum malzemeleriyle birlikte doner
    @GetMapping("/dokum/{id}")
    public DokumDetayResponse detay(@PathVariable Integer id) {
        return dokumService.detay(id);
    }
}
