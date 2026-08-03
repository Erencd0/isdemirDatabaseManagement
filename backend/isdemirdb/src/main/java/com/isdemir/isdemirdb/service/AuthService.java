package com.isdemir.isdemirdb.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.isdemir.isdemirdb.dto.LoginRequest;
import com.isdemir.isdemirdb.dto.LoginResponse;
import com.isdemir.isdemirdb.entity.Kullanici;
import com.isdemir.isdemirdb.exception.GecersizKimlikException;
import com.isdemir.isdemirdb.exception.YetkisizGirisException;
import com.isdemir.isdemirdb.repository.KullaniciRepository;

import lombok.RequiredArgsConstructor;

// Login is mantiginin tamami bu katmanda. Controller sadece bu servisi cagirir.
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KullaniciRepository kullaniciRepository;

    public LoginResponse login(LoginRequest request) {

        // Kullanici adi / parola eslesmesini ara -> yoksa 401
        Kullanici kullanici = kullaniciRepository
                .findByKullaniciAdiAndKullaniciParola(request.getKullaniciAdi(), request.getKullaniciParola())
                .orElseThrow(() -> new GecersizKimlikException("Kullanıcı adı veya parola hatalı"));

        // Eslesme var ama rol_adi NULL/bos -> 403
        if (kullanici.getRolAdi() == null || kullanici.getRolAdi().trim().isEmpty()) {
            throw new YetkisizGirisException("Bu kullanıcının giriş yetkisi yok");
        }

        // rol_adi dolu -> "kv1,kv3" gibi metni diziye parse et
        List<String> roller = Arrays.stream(kullanici.getRolAdi().split(","))
                .map(String::trim)
                .filter(rol -> !rol.isEmpty())
                .collect(Collectors.toList());

        return new LoginResponse(
                kullanici.getKullaniciId(),
                kullanici.getKullaniciAdi(),
                roller);
    }
}
