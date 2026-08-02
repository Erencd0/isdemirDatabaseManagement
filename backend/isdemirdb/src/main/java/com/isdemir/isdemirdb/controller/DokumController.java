package com.isdemir.isdemirdb.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

import com.isdemir.isdemirdb.dto.DokumDetayResponse;
import com.isdemir.isdemirdb.entity.Dokum;
import com.isdemir.isdemirdb.entity.Malzeme;
import com.isdemir.isdemirdb.entity.MalzemeKullanim;
import com.isdemir.isdemirdb.repository.DokumRepository;
import com.isdemir.isdemirdb.repository.MalzemeKullanimRepository;
import com.isdemir.isdemirdb.repository.MalzemeRepository;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174" })
public class DokumController {

    private final DokumRepository dokumRepository;
    private final MalzemeKullanimRepository malzemeKullanimRepository;
    private final MalzemeRepository malzemeRepository;

    public DokumController(DokumRepository dokumRepository,
            MalzemeKullanimRepository malzemeKullanimRepository,
            MalzemeRepository malzemeRepository) {
        this.dokumRepository = dokumRepository;
        this.malzemeKullanimRepository = malzemeKullanimRepository;
        this.malzemeRepository = malzemeRepository;
    }

    // Tum dokumleri listeler
    @GetMapping("/dokum")
    public List<Dokum> listele() {
        return dokumRepository.findAll();
    }

    // Yeni dokum ekler
    @PostMapping("/dokum")
    public ResponseEntity<?> ekle(@RequestBody Dokum dokum) {
        // Zaman degerleri kendi icinde mantikli sirada olmali; degilse kaydetme, 400 don
        String zamanHatasi = zamanlariDogrula(dokum);
        if (zamanHatasi != null) {
            return ResponseEntity.badRequest().body(zamanHatasi);
        }
        // Ayni konverterdeki dokumler sirali olmali: yeni dokum, o konverterin onceki
        // dokumunden once olamaz. Farkli konverterler paralel calisir, aralarinda kural yok.
        String siraHatasi = oncekiDokumleSiraDogrula(dokum);
        if (siraHatasi != null) {
            return ResponseEntity.badRequest().body(siraHatasi);
        }
        // Kayit zamani gonderilmediyse su anki zaman atanir
        if (dokum.getKayitZamani() == null) {
            dokum.setKayitZamani(LocalDateTime.now());
        }
        // ID'yi elle ata: mevcut en buyuk id + 1. Kayit basarisiz olursa
        // hicbir sequence ilerlemez, bir sonraki POST ayni numarayi tekrar dener.
        dokum.setDokumId(dokumRepository.findMaxDokumId() + 1);

        // dokum_no kurali sonrakiDokumNo() icinde. POST ile "sonraki-no" endpoint'i
        // ayni hesabi kullanir, boylece ekranda gosterilen numara ile kaydedilen ayni olur.
        dokum.setDokumNo(sonrakiDokumNo(dokum.getKonverterNo()));

        return ResponseEntity.ok(dokumRepository.save(dokum));
    }

    // Dokumun islem zamanlari, sirasiyla (kronolojik) dizi olarak.
    // Sira: hurda sarj basla -> bitis -> ana uflemeye basla -> ana ufleme bitis -> dokum
    private LocalDateTime[] zamanDizisi(Dokum d) {
        return new LocalDateTime[] {
                d.getHurdaSarjBaslamaZamani(),
                d.getHurdaSarjBitisZamani(),
                d.getAnaUflemeyeBaslamaZamani(),
                d.getAnaUflemeBitisZamani(),
                d.getDokumZamani(),
        };
    }

    private static final String[] ZAMAN_ADLARI = {
            "Hurda Şarj Başlama",
            "Hurda Şarj Bitiş",
            "Ana Üflemeye Başlama",
            "Ana Üfleme Bitiş",
            "Döküm Zamanı",
    };

    // Dokum islem zamanlarinin kendi icinde kronolojik sirada olup olmadigini kontrol eder.
    // Sonraki bir zaman oncekinden ONCE ise hata mesaji doner, tutarliysa null. Null alanlar atlanir.
    private String zamanlariDogrula(Dokum d) {
        LocalDateTime[] zamanlar = zamanDizisi(d);
        for (int i = 1; i < zamanlar.length; i++) {
            if (zamanlar[i] != null && zamanlar[i - 1] != null
                    && zamanlar[i].isBefore(zamanlar[i - 1])) {
                return ZAMAN_ADLARI[i] + " zamanı, " + ZAMAN_ADLARI[i - 1] + " zamanından önce olamaz";
            }
        }
        return null;
    }

    // Dokumun (null olmayan) en erken islem zamani
    private LocalDateTime enErkenZaman(Dokum d) {
        LocalDateTime enErken = null;
        for (LocalDateTime t : zamanDizisi(d)) {
            if (t != null && (enErken == null || t.isBefore(enErken))) {
                enErken = t;
            }
        }
        return enErken;
    }

    // Dokumun (null olmayan) en gec islem zamani
    private LocalDateTime enGecZaman(Dokum d) {
        LocalDateTime enGec = null;
        for (LocalDateTime t : zamanDizisi(d)) {
            if (t != null && (enGec == null || t.isAfter(enGec))) {
                enGec = t;
            }
        }
        return enGec;
    }

    // Ayni konverterdeki dokumler sirali oldugundan, yeni dokumun zamanlari o konverterin
    // bir onceki dokumunun zamanlarindan once olamaz (dokumler cakisamaz). Yeni dokumun en
    // erken zamani, onceki dokumun en gec zamanindan onceyse hata doner. Farkli konverterler
    // (orn. 6100001 ile 6300001) birbirinden bagimsizdir, kontrol edilmez.
    private String oncekiDokumleSiraDogrula(Dokum yeni) {
        int taban = 6_000_000 + yeni.getKonverterNo() * 100_000;
        Dokum onceki = dokumRepository
                .findFirstByDokumNoBetweenOrderByDokumNoDesc(taban + 1, taban + 99_999)
                .orElse(null);
        if (onceki == null) {
            return null; // bu konverterin ilk dokumu
        }
        LocalDateTime oncekiSon = enGecZaman(onceki);
        LocalDateTime yeniIlk = enErkenZaman(yeni);
        if (oncekiSon != null && yeniIlk != null && yeniIlk.isBefore(oncekiSon)) {
            return "Bu dökümün zamanları, aynı konverterdeki önceki dökümün (No: "
                    + onceki.getDokumNo() + ") zamanlarından önce olamaz";
        }
        return null;
    }

    // Bir sonraki dokum_no'yu hesaplar.
    // Kural: 7 haneli, 6 ile baslar, 2. hane konverter no (secilen rol kv1/2/3 -> 1/2/3),
    // kalan 5 hane o konvertere ozel sirali sayac (00001'den baslar).
    // Ornek: konverter=3 (kv3) -> ilk dokum 6300001, sonraki 6300002 ...
    private Integer sonrakiDokumNo(Integer konverterNo) {
        int taban = 6_000_000 + konverterNo * 100_000; // orn. konverter 3 -> 6300000
        // Sadece bu konverterin bandindaki (taban+1 .. taban+99999) numaralara bakilir,
        // boylece onek her zaman dogru olur (kv1 -> 61..., kv2 -> 62..., kv3 -> 63...).
        Integer sonDokumNo = dokumRepository.findMaxDokumNoInRange(taban + 1, taban + 99_999);
        // Bu konverterin ilk dokumu ise taban+1 (6300001), degilse son numaranin +1'i
        return sonDokumNo == 0 ? taban + 1 : sonDokumNo + 1;
    }

    // Kaydetmeden once, bu konverterin alacagi bir sonraki dokum_no'yu doner.
    // Frontend formda "degistirilemez" sekilde gostermek icin kullanir.
    @GetMapping("/dokum/sonraki-no")
    public Integer sonrakiNo(@RequestParam Integer konverterNo) {
        return sonrakiDokumNo(konverterNo);
    }

    // Bir dokume malzeme ekler. dokum_id path'ten alinir, body'deki
    // dokumId gonderilse bile ezilir. islem_zamani'na dokunulmaz (DB doldurur).
    // malzeme_verilis_tarihi ve kullanici_id body'den gelir.
    @PostMapping("/dokum/{id}/malzeme")
    public ResponseEntity<MalzemeKullanim> malzemeEkle(@PathVariable Integer id,
            @RequestBody MalzemeKullanim malzemeKullanim) {
        // Dokum yoksa FK hatasi almadan once temiz 404 don
        if (!dokumRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        malzemeKullanim.setDokumId(id);
        MalzemeKullanim kaydedilen = malzemeKullanimRepository.save(malzemeKullanim);
        return ResponseEntity.ok(kaydedilen);
    }

    // Bir dokumdeki malzeme kullanimini gunceller (malzeme_kodu, miktar, verilis tarihi).
    // kullanim_id ile bulunur; ilgili dokume ait degilse 404 don (baska dokumun kaydi ezilmesin).
    @PutMapping("/dokum/{id}/malzeme/{kullanimId}")
    public ResponseEntity<MalzemeKullanim> malzemeGuncelle(@PathVariable Integer id,
            @PathVariable Integer kullanimId,
            @RequestBody MalzemeKullanim gelen) {
        return malzemeKullanimRepository.findById(kullanimId)
                .filter(mevcut -> id.equals(mevcut.getDokumId()))
                .map(mevcut -> {
                    mevcut.setMalzemeKodu(gelen.getMalzemeKodu());
                    mevcut.setMiktar(gelen.getMiktar());
                    mevcut.setMalzemeVerilisTarihi(gelen.getMalzemeVerilisTarihi());
                    if (gelen.getKullaniciId() != null) {
                        mevcut.setKullaniciId(gelen.getKullaniciId());
                    }
                    return ResponseEntity.ok(malzemeKullanimRepository.save(mevcut));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Bir dokumdeki malzeme kullanimini siler. Ilgili dokume ait degilse 404 don.
    @DeleteMapping("/dokum/{id}/malzeme/{kullanimId}")
    public ResponseEntity<Void> malzemeSil(@PathVariable Integer id, @PathVariable Integer kullanimId) {
        return malzemeKullanimRepository.findById(kullanimId)
                .filter(mevcut -> id.equals(mevcut.getDokumId()))
                .map(mevcut -> {
                    malzemeKullanimRepository.delete(mevcut);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // "Detay gor": dokumu, o dokume eklenmis tum malzemeleriyle birlikte doner
    @GetMapping("/dokum/{id}")
    public ResponseEntity<DokumDetayResponse> detay(@PathVariable Integer id) {
        return dokumRepository.findById(id)
                .map(dokum -> {
                    List<MalzemeKullanim> malzemeler = malzemeKullanimRepository.findByDokumId(id);
                    malzemeAdlariniDoldur(malzemeler);
                    return ResponseEntity.ok(new DokumDetayResponse(dokum, malzemeler));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Malzeme kullanimlarina, malzeme_kodu'na karsilik gelen malzeme_adi ve malzeme_turu'nu
    // doldurur. malzeme_tablosu tek sorguda cekilip kod -> Malzeme haritasi kurulur (N+1 olmaz).
    private void malzemeAdlariniDoldur(List<MalzemeKullanim> malzemeler) {
        if (malzemeler.isEmpty()) {
            return;
        }
        List<Integer> kodlar = malzemeler.stream()
                .map(MalzemeKullanim::getMalzemeKodu)
                .filter(k -> k != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, Malzeme> kodMalzeme = malzemeRepository.findByMalzemeKoduIn(kodlar).stream()
                .collect(Collectors.toMap(Malzeme::getMalzemeKodu, m -> m, (var1, var2) -> var1));
        for (MalzemeKullanim m : malzemeler) {
            Malzeme malzeme = kodMalzeme.get(m.getMalzemeKodu());
            if (malzeme != null) {
                m.setMalzemeAdi(malzeme.getMalzemeAdi());
                m.setMalzemeTuru(malzeme.getMalzemeTuru());
            }
        }
    }
}
