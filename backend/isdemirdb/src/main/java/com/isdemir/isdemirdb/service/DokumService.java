package com.isdemir.isdemirdb.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.isdemir.isdemirdb.dto.DokumDetayResponse;
import com.isdemir.isdemirdb.entity.Dokum;
import com.isdemir.isdemirdb.entity.Malzeme;
import com.isdemir.isdemirdb.entity.MalzemeKullanim;
import com.isdemir.isdemirdb.exception.GecersizVeriException;
import com.isdemir.isdemirdb.exception.KayitBulunamadiException;
import com.isdemir.isdemirdb.repository.DokumRepository;
import com.isdemir.isdemirdb.repository.MalzemeKullanimRepository;
import com.isdemir.isdemirdb.repository.MalzemeRepository;

import lombok.RequiredArgsConstructor;

// Dokum ve dokume bagli malzeme kullanimlarina ait tum is mantigi bu katmanda.
// Controller sadece bu servisi cagirir; hata -> HTTP kodu cevrimi GlobalExceptionHandler'da.
@Service
@RequiredArgsConstructor
public class DokumService {

    private final DokumRepository dokumRepository;
    private final MalzemeKullanimRepository malzemeKullanimRepository;
    private final MalzemeRepository malzemeRepository;

    // Tum dokumleri listeler
    public List<Dokum> tumDokumler() {
        return dokumRepository.findAll();
    }

    // Yeni dokum ekler. Zaman/sira kurallari ihlal edilirse GecersizVeriException (-> 400).
    public Dokum ekle(Dokum dokum) {
        // Zaman degerleri kendi icinde mantikli sirada olmali; degilse kaydetme.
        String zamanHatasi = zamanlariDogrula(dokum);
        if (zamanHatasi != null) {
            throw new GecersizVeriException(zamanHatasi);
        }
        // Ayni konverterdeki dokumler sirali olmali: yeni dokum, o konverterin onceki
        // dokumunden once olamaz. Farkli konverterler paralel calisir, aralarinda kural yok.
        String siraHatasi = oncekiDokumleSiraDogrula(dokum);
        if (siraHatasi != null) {
            throw new GecersizVeriException(siraHatasi);
        }
        // Kayit zamani gonderilmediyse su anki zaman atanir
        if (dokum.getKayitZamani() == null) {
            dokum.setKayitZamani(LocalDateTime.now());
        }
        // ID'yi elle ata: mevcut en buyuk id + 1. Kayit basarisiz olursa
        // hicbir sequence ilerlemez, bir sonraki ekleme ayni numarayi tekrar dener.
        dokum.setDokumId(dokumRepository.findMaxDokumId() + 1);

        // dokum_no kurali sonrakiDokumNo() icinde. "sonraki-no" endpoint'i ayni hesabi
        // kullanir, boylece ekranda gosterilen numara ile kaydedilen ayni olur.
        dokum.setDokumNo(sonrakiDokumNo(dokum.getKonverterNo()));

        return dokumRepository.save(dokum);
    }

    // Bir sonraki dokum_no'yu hesaplar.
    // Kural: 7 haneli, 6 ile baslar, 2. hane konverter no (secilen rol kv1/2/3 -> 1/2/3),
    // kalan 5 hane o konvertere ozel sirali sayac (00001'den baslar).
    // Ornek: konverter=3 (kv3) -> ilk dokum 6300001, sonraki 6300002 ...
    // Frontend formda "degistirilemez" numarayi gostermek icin de cagirir.
    public Integer sonrakiDokumNo(Integer konverterNo) {
        int taban = 6_000_000 + konverterNo * 100_000; // orn. konverter 3 -> 6300000
        // Sadece bu konverterin bandindaki (taban+1 .. taban+99999) numaralara bakilir,
        // boylece onek her zaman dogru olur (kv1 -> 61..., kv2 -> 62..., kv3 -> 63...).
        Integer sonDokumNo = dokumRepository.findMaxDokumNoInRange(taban + 1, taban + 99_999);
        // Bu konverterin ilk dokumu ise taban+1 (6300001), degilse son numaranin +1'i
        return sonDokumNo == 0 ? taban + 1 : sonDokumNo + 1;
    }

    // Bir dokume malzeme ekler. dokumId path'ten alinir, body'deki dokumId ezilir.
    // Dokum yoksa KayitBulunamadiException (-> 404).
    public MalzemeKullanim malzemeEkle(Integer dokumId, MalzemeKullanim malzemeKullanim) {
        Dokum dokum = dokumRepository.findById(dokumId)
                .orElseThrow(() -> new KayitBulunamadiException("Döküm bulunamadı"));
        // Malzeme, dokumden once verilemez: verilis tarihi dokum zamanindan once olamaz.
        String zamanHatasi = malzemeZamaniDogrula(dokum, malzemeKullanim.getMalzemeVerilisTarihi());
        if (zamanHatasi != null) {
            throw new GecersizVeriException(zamanHatasi);
        }
        malzemeKullanim.setDokumId(dokumId);
        return malzemeKullanimRepository.save(malzemeKullanim);
    }

    // Bir dokumdeki malzeme kullanimini gunceller (malzeme_kodu, miktar, verilis tarihi).
    // kullanim_id ile bulunur; ilgili dokume ait degilse KayitBulunamadiException (-> 404),
    // boylece baska dokumun kaydi ezilmez.
    public MalzemeKullanim malzemeGuncelle(Integer dokumId, Integer kullanimId, MalzemeKullanim gelen) {
        MalzemeKullanim mevcut = malzemeKullanimRepository.findById(kullanimId)
                .filter(m -> dokumId.equals(m.getDokumId()))
                .orElseThrow(() -> new KayitBulunamadiException("Malzeme kullanımı bulunamadı"));
        Dokum dokum = dokumRepository.findById(dokumId)
                .orElseThrow(() -> new KayitBulunamadiException("Döküm bulunamadı"));
        // Guncellenen verilis tarihi de dokum zamanindan once olamaz.
        String zamanHatasi = malzemeZamaniDogrula(dokum, gelen.getMalzemeVerilisTarihi());
        if (zamanHatasi != null) {
            throw new GecersizVeriException(zamanHatasi);
        }
        mevcut.setMalzemeKodu(gelen.getMalzemeKodu());
        mevcut.setMiktar(gelen.getMiktar());
        mevcut.setMalzemeVerilisTarihi(gelen.getMalzemeVerilisTarihi());
        if (gelen.getKullaniciId() != null) {
            mevcut.setKullaniciId(gelen.getKullaniciId());
        }
        return malzemeKullanimRepository.save(mevcut);
    }

    // Bir dokumdeki malzeme kullanimini siler. Ilgili dokume ait degilse
    // KayitBulunamadiException (-> 404).
    public void malzemeSil(Integer dokumId, Integer kullanimId) {
        MalzemeKullanim mevcut = malzemeKullanimRepository.findById(kullanimId)
                .filter(m -> dokumId.equals(m.getDokumId()))
                .orElseThrow(() -> new KayitBulunamadiException("Malzeme kullanımı bulunamadı"));
        malzemeKullanimRepository.delete(mevcut);
    }

    // "Detay gor": dokumu, o dokume eklenmis tum malzemeleriyle birlikte doner.
    // Dokum yoksa KayitBulunamadiException (-> 404).
    public DokumDetayResponse detay(Integer id) {
        Dokum dokum = dokumRepository.findById(id)
                .orElseThrow(() -> new KayitBulunamadiException("Döküm bulunamadı"));
        List<MalzemeKullanim> malzemeler = malzemeKullanimRepository.findByDokumId(id);
        malzemeAdlariniDoldur(malzemeler);
        return new DokumDetayResponse(dokum, malzemeler);
    }

    // ---- Dahili yardimcilar ----

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

    // Malzeme verilis tarihi, dokumun son zamani olan dokum_zamani'ndan once olamaz.
    // Iki taraftan biri null ise (verilis tarihi verilmemis ya da dokum zamani bos) atlanir.
    private String malzemeZamaniDogrula(Dokum dokum, LocalDateTime verilisTarihi) {
        LocalDateTime dokumZamani = dokum.getDokumZamani();
        if (verilisTarihi != null && dokumZamani != null && verilisTarihi.isBefore(dokumZamani)) {
            return "Malzeme veriliş tarihi, döküm zamanından önce olamaz";
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
