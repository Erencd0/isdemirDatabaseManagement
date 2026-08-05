# İsdemirdb – Postman Koleksiyonu

Backend servisinin (Spring Boot, `http://localhost:8080`) Postman test koleksiyonu.
Seviye otomasyonunun gönderdiği döküm/malzeme verisini taklit eder; **zaman verileri tutarsızsa** backend HTTP 400 ile özel bir mesaj döner. Tüm zaman doğrulaması backend service katmanındadır.

## İçe aktarma (import)
1. Postman > **Import** > `isdemirdb.postman_collection.json` dosyasını seç.
2. Koleksiyona sağ tık > **Variables** (veya koleksiyon adına tıkla > Variables sekmesi):
   - `kullaniciAdi`, `kullaniciParola` → giriş yetkisi olan gerçek bir kullanıcı
   - `kullaniciId`, `konverterNo` → o kullanıcıya uygun değerler (varsayılan 1)

## Çalıştırma sırası
1. Backend'i başlat: `mvn spring-boot:run`
2. Klasörleri sırayla çalıştır:
   - **1 – Kimlik**: 200 / 401 / 403 senaryoları
   - **2 – Malzeme**: tür ve malzeme listeleme
   - **3 – Döküm (Geçerli)**: `POST Döküm - Geçerli`, dönen `dokumId`'yi otomatik olarak `{{dokumId}}`'ye yazar
   - **4 – Zaman Hatası**: ters/çakışan zaman → 400 + özel mesaj
   - **5 – Malzeme Kullanım**: `{{dokumId}}` üzerinden ekle/güncelle/sil (`kullanimId` otomatik zincirlenir)
   - **6 – 404**: olmayan kayıt

> Toplu çalıştırma için: koleksiyon > **Run** (Collection Runner). Sonuçlar ve `console.log` mesajları **View > Show Postman Console**'da görünür.

## Zaman hata senaryoları (özel mesajlar)
| Senaryo | Sonuç | Mesaj |
|---|---|---|
| Döküm zamanı ana üflemeden önce | 400 | `... zamanı, ... zamanından önce olamaz` |
| Hurda şarj bitiş, başlangıçtan önce | 400 | `Hurda Şarj Bitiş zamanı, Hurda Şarj Başlama zamanından önce olamaz` |
| Yeni döküm önceki dökümden önce | 400 | `Bu dökümün zamanları, aynı konverterdeki önceki dökümün (No: ...) zamanlarından önce olamaz` |
| Malzeme veriliş, döküm zamanından önce | 400 | `Malzeme veriliş tarihi, döküm zamanından önce olamaz` |

> Not: `malzemeKodu` ve `tur` gibi değerleri kendi veritabanındaki gerçek kayıtlarla değiştir.
