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


