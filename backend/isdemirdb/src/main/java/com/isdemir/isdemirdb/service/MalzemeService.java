package com.isdemir.isdemirdb.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.isdemir.isdemirdb.entity.Malzeme;
import com.isdemir.isdemirdb.repository.MalzemeRepository;

// Malzeme listeleme is mantigi bu katmanda. Controller sadece bu servisi cagirir.
@Service
public class MalzemeService {

    private final MalzemeRepository malzemeRepository;

    public MalzemeService(MalzemeRepository malzemeRepository) {
        this.malzemeRepository = malzemeRepository;
    }

    // Benzersiz malzeme turlerini listeler (combobox icin)
    public List<String> turler() {
        return malzemeRepository.findTurler();
    }

    // Secilen ture ait aktif malzemeleri listeler
    public List<Malzeme> tureGoreListele(String tur) {
        return malzemeRepository.findByMalzemeTuruAndAktifPasifTrue(tur);
    }
}
