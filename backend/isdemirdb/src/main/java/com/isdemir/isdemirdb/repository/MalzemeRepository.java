package com.isdemir.isdemirdb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.isdemir.isdemirdb.entity.Malzeme;

@Repository
public interface MalzemeRepository extends JpaRepository<Malzeme, Integer> {

    // Aktif malzemelerin benzersiz turlerini alfabetik olarak dondurur
    @Query("SELECT DISTINCT m.malzemeTuru FROM Malzeme m "
            + "WHERE m.aktifPasif = true AND m.malzemeTuru IS NOT NULL "
            + "ORDER BY m.malzemeTuru")
    List<String> findTurler();

    // Belirli bir ture ait aktif malzemeleri dondurur
    List<Malzeme> findByMalzemeTuruAndAktifPasifTrue(String malzemeTuru);

    // Verilen malzeme kodlarina ait malzemeleri dondurur ("detay gor"da kod -> ad eslemesi icin)
    List<Malzeme> findByMalzemeKoduIn(List<Integer> malzemeKodlari);
}
