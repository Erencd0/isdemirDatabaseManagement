package com.isdemir.isdemirdb.dto;

import java.util.List;

import com.isdemir.isdemirdb.entity.Dokum;
import com.isdemir.isdemirdb.entity.MalzemeKullanim;

import lombok.AllArgsConstructor;
import lombok.Getter;

// "Detay gor" icin: dokumun kendisi + o dokume eklenmis malzemeler
@Getter
@AllArgsConstructor
public class DokumDetayResponse {

    private Dokum dokum;
    private List<MalzemeKullanim> malzemeler;
}
