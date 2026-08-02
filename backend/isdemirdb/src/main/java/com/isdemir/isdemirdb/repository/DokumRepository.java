package com.isdemir.isdemirdb.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.isdemir.isdemirdb.entity.Dokum;

@Repository
public interface DokumRepository extends JpaRepository<Dokum, Integer> {

    // Bir bandin (ayni konverter) en yuksek dokum_no'lu, yani en son dokumunu getirir.
    // Yeni dokumun zamanlarini bu son dokumle karsilastirmak icin kullanilir.
    Optional<Dokum> findFirstByDokumNoBetweenOrderByDokumNoDesc(Integer alt, Integer ust);

    // Mevcut en buyuk dokum_id degerini dondurur (tablo bossa 0)
    @Query("SELECT COALESCE(MAX(d.dokumId), 0) FROM Dokum d")
    Integer findMaxDokumId();

    // Belirli bir dokum_no bandindaki en buyuk dokum_no'yu dondurur (bandda kayit yoksa 0).
    // Sayac konverter_no KOLONUNA gore degil, dokum_no'nun sayisal bandina gore ilerler;
    // boylece eski/tutarsiz konverter_no verisi bir sonraki numarayi bozamaz.
    // Orn. konverter=1 -> band [6100001, 6199999].
    @Query("SELECT COALESCE(MAX(d.dokumNo), 0) FROM Dokum d WHERE d.dokumNo BETWEEN :alt AND :ust")
    Integer findMaxDokumNoInRange(Integer alt, Integer ust);
}
