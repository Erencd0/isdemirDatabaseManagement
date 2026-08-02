\# İsdemirdb Projesi - Durum Özeti



\## Proje Yapısı

\- Backend: `backend/isdemirdb` (Spring Boot, pom.xml burada)

\- Frontend: `frontend/` (React + Vite + Tailwind, kurulum tamam ama login ekranı henüz yazılmadı)

\- Veritabanı: PostgreSQL



\## Ne Amaçlı Proje

Staj örnek projesi — bir kullanıcı giriş (login) sistemi. Güvenlik/hashing/validasyon gerekmiyor, basit tutuluyor.



\## Veritabanı Şeması

`kullanici` tablosu:

\- `kullanici\_id` (int, PK, identity)

\- `kullanici\_adi` (varchar, unique)

\- `kullanici\_parola` (varchar, düz metin — hash yok)

\- `rol\_adi` (varchar, nullable — "kv1" ya da "kv1,kv3" gibi virgülle ayrılmış birden fazla rol içerebilir; bazı kullanıcılarda NULL/boş)



Tabloda 7 kullanıcı var.



\## Backend - TAMAMLANDI ✅

\- Kullanici entity + repository yazıldı

\- `POST /api/login` endpoint'i çalışıyor:

&#x20; - Kullanıcı adı/parola eşleşmiyorsa → \*\*401\*\* ("Kullanıcı adı veya parola hatalı")

&#x20; - Eşleşme var ama `rol\_adi` NULL/boş ise → \*\*403\*\* ("Bu kullanıcının giriş yetkisi yok") — giriş yaptırma

&#x20; - Eşleşme var ve rol\_adi doluysa → \*\*200\*\*, response: `kullanici\_id`, `kullanici\_adi`, `roller` (rol\_adi parse edilip diziye çevrilmiş halde, örn. "kv1,kv3" → `\["kv1","kv3"]`). Parola response'a dahil edilmiyor.

\- `application.properties` datasource ayarları doğrulandı

\- Postman ile 4 senaryo test edilecek/edildi: rolü olan kullanıcı, çoklu rollü kullanıcı, rolsüz kullanıcı, yanlış parola

\- CORS ayarı HENÜZ EKLENMEDİ (frontend kurulunca eklenecek)

\-Dokum entity + repository + GET /api/dokum — sadece listeleme

\-POST /api/dokum — döküm ekleme

\-Malzeme entity + GET /api/malzeme/turler ve GET /api/malzeme?tur=...

\-MalzemeKullanim entity + POST /api/dokum/{id}/malzeme

\-GET /api/dokum/{id} — dökümü malzemeleriyle birlikte döndürme

\## Frontend - YAPILDI 

Login ekranı TEK EKRAN olacak (rol seçimi ayrı ekran DEĞİL):

\- Kullanıcı adı, parola input'ları + rol combobox'ı aynı formda, alt alta

\- Combobox başta boş/disabled (roller henüz bilinmiyor, giriş denemeden dolmayacak)

\- "Giriş" butonuna basılınca backend'e istek atılır

\- 401/403 dönerse ilgili hata mesajı gösterilir

\- Başarılı dönerse: combobox, dönen `roller` dizisiyle doldurulur

&#x20; - \*\*ÖNEMLİ: tek rol olsa bile OTOMATİK SEÇİLMEZ\*\* — kullanıcı yine de comboboxtan seçim yapmak zorunda

\- Rol seçildikten sonra giriş tamamlanır, seçilen rol state/localStorage'a kaydedilir

\- Backend adresi: `http://localhost:8080/api/login`

\- CORS: backend tarafında frontend'in çalıştığı porta (muhtemelen Vite 5173) izin verilmesi lazım — bu adım henüz atlanmıştı, frontend'e geçerken eklenmeli



\## Ortam / Araçlar

\- Backend: VS Code'da geliştiriliyor, `mvn spring-boot:run` ile çalıştırılıyor

\- Java 17 (Temurin JDK), Maven PATH'e eklendi

\- Frontend: React + Tailwind kurulumu tamam, henüz login componenti yazılmadı

/## DASHBOARD FRONTEND YAPILACAKLAR
!!! once hali hazirdaki frontend temasina bak belirli seyler yaptik ona gore uygun yap!!!
!!! her bir maddeyi adim adim yapacaksin ve ben izin vermeden digerine gecmeyeceksin once ben kontrol edecegim!!!
!!! ilk login page yaptiginda login olduktan sonra giris yapildi diye bir sayfaya atiyordu o sayfaya gerek yok artik ama istersen kisa bir loading sekansi koyabilirsin!!!

1- Sayfanin ust kisimlarindan birinde giris yapan kisinin bilgileri ve cikis yap tusu olucak

2- Ana sayfada dokumleri listele ve yeni dokum butonu olacak bu butonlarin altinda ise:
	dokum_tablosu tablosundaki degerler el ile girilecek yerler olacak (malzeme kodu haric) ve altinda kaydet butonu olucak BU KISIM HER ZAMAN OLACAK YANI BI TUSA BASINCA CIKMAYACAK

3-dokumleri listele butonuna basinca veritabanindaki dokumlerin basit 2-3 bilgisi  pop up bir sekmede gosterilecek ve her dokumun yaninda detay gor butonu olucak buna basilinca ek bir popup acilacak ve basilan dokumun kendi tum detaylari ve malzemeleride gorunecek

4-dokum bilgileri girilen sayfanin altinda ise malzeme ekle sayfasi olacak bu sayfanin hemen ustunde 3 buton olacak Konverter Katki, Pota Katki ve Hurda Katki seklinde once kisi bu butonlara basarak malzeme bilgilerini girecek (sonucta her katkinin mazlemeleri farkli basilana gore malzeme kodu ve isimleri cikicak comboboxta)

5-hemen ardindan secilen katkiya dahil malzeme bilgileri girilecek ve ve bu bilgiler o anki dokume kaydedilecek ve ayrica hemen bu bilgilerin girildigi yerin altindaki malzeme listesinde de gosterilecek

6-bu listedeki malzemlerin saginda buton olucak 2 tane bir tane silmek icin bir tane malzemeyi guncellemek icin

