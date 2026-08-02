package com.isdemir.isdemirdb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.isdemir.isdemirdb.entity.MalzemeKullanim;

@Repository
public interface MalzemeKullanimRepository extends JpaRepository<MalzemeKullanim, Integer> {

    // Belirli bir dokume ait tum malzeme kullanimlarini dondurur ("detay gor")
    List<MalzemeKullanim> findByDokumId(Integer dokumId);
}
