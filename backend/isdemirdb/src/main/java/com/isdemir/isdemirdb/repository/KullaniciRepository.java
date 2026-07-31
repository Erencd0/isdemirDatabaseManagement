package com.isdemir.isdemirdb.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.isdemir.isdemirdb.entity.Kullanici;

@Repository
public interface KullaniciRepository extends JpaRepository<Kullanici, Integer> {

    // kullanici_adi ve kullanici_parola ile eslesen kaydi bulur
    Optional<Kullanici> findByKullaniciAdiAndKullaniciParola(String kullaniciAdi, String kullaniciParola);
}
