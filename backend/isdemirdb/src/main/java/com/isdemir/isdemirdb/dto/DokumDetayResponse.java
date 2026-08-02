package com.isdemir.isdemirdb.dto;

import java.util.List;

import com.isdemir.isdemirdb.entity.Dokum;
import com.isdemir.isdemirdb.entity.MalzemeKullanim;

// "Detay gor" icin: dokumun kendisi + o dokume eklenmis malzemeler
public class DokumDetayResponse {

    private Dokum dokum;
    private List<MalzemeKullanim> malzemeler;

    public DokumDetayResponse(Dokum dokum, List<MalzemeKullanim> malzemeler) {
        this.dokum = dokum;
        this.malzemeler = malzemeler;
    }

    public Dokum getDokum() {
        return dokum;
    }

    public List<MalzemeKullanim> getMalzemeler() {
        return malzemeler;
    }
}
