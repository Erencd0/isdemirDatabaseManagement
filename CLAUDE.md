# JWT Authentication Roadmap

## Amaç

Projeye JWT tabanlı giriş sistemi eklemek.

Giriş yapan kullanıcılar **Access Token** ve **Refresh Token** alacak.

Sadece giriş yapan kullanıcılar döküm ve malzeme işlemlerini gerçekleştirebilecek.

> **Not:**
> Bu proje staj projesi olduğu için ilk aşamada şifreler hashlenmeyecek.
> Gerekirse daha sonra BCrypt eklenebilir.

---

# Aşama 1 - Kullanıcı Yapısını Kontrol Et YAPILDI

`kullanici` tablosunda aşağıdaki alanlar bulunmalı.

```
kullanici_id
kullanici_adi
kullanici_parola
rol_adi
```

Bu aşamada amaç:

- Kullanıcı oluşturabilmek
- Login için kullanıcıyı veritabanından okuyabilmek

Henüz JWT kullanılmayacak.

---

# Aşama 2 - Refresh Token Tablosunu Oluştur YAPILDI

Yeni tablo:

```
refresh_tokens
```

Kolonlar:

```
id
kullanici_id
token
bitis_zamani
olusturulma_zamani
aktif
```

İlişki:

```
refresh_tokens.kullanici_id
            ↓
kullanici.kullanici_id
```

Foreign Key kullanılacak.

Amaç:

Bir kullanıcının Refresh Token bilgisini veritabanında saklayabilmek.

---

# Aşama 3 - Spring Security'yi Projeye Ekle YAPILDI

Spring Security dependency'sini ekle.

Henüz authentication yapılmayacak.

`SecurityConfig` oluştur.

Bütün endpointler geçici olarak açık olsun.

```java
permitAll()
```

Amaç:

Spring Security altyapısını hazırlamak.

---

# Aşama 4 - Login Endpointi YAPILDI

Endpoint

```
POST /auth/login
```

Request

```json
{
    "username": "eren",
    "password": "123456"
}
```

Bu aşamada yapılacaklar:

- Kullanıcıyı bul
- Şifreyi kontrol et
- Başarılıysa girişe izin ver

Henüz JWT oluşturulmayacak.

---

# Aşama 5 - Access Token Üret YAPILDI

Login başarılıysa JWT Access Token oluştur.

Response

```json
{
    "accessToken": "..."
}
```

Henüz Refresh Token kullanılmayacak.

Amaç:

JWT üretme mantığını öğrenmek.

---

# Aşama 6 - JWT Filter Yaz YAPILDI

Her istek aşağıdaki Authorization Header ile gelecek.

```
Authorization

Bearer <access_token>
```

> Bu token **Postman içerisinde Authorization → Bearer Token alanına manuel olarak yapıştırılacaktır.**
> Otomatik token ekleme veya otomatik yenileme yapılmayacaktır.

JWT Filter işlemleri:

↓

Authorization Header'ı oku

↓

Token doğrula

↓

Kullanıcıyı belirle

↓

SecurityContext içerisine yerleştir

Bu aşamadan sonra login olmadan korunan endpointlere erişilemeyecek.

---

# Aşama 7 - Refresh Token Sistemi YAPILDI

Login başarılı olunca iki token oluştur.

```
Access Token

Refresh Token
```

Refresh Token

```
refresh_tokens
```

tablosuna kaydedilecek.

Response

```json
{
    "accessToken": "...",
    "refreshToken": "..."
}
```

---

# Aşama 8 - Refresh Endpointi YAPILDI

Endpoint

```
POST /auth/refresh
```

Request

```json
{
    "refreshToken": "..."
}
```

İşleyiş

- Token DB'de var mı?
- aktif = true mı?
- Süresi dolmuş mu?

Hepsi doğruysa

↓

Yeni Access Token üret.

↓

Response olarak geri döndür.

> Yeni Access Token yine Postman üzerinde eski Bearer Token silinerek manuel olarak eklenecektir.

---
 
# Aşama 9 - Logout YAPILDI

Endpoint

```
POST /auth/logout
```

Logout olduğunda

```
aktif = false
```

yapılacak.

Artık bu Refresh Token kullanılarak yeni Access Token üretilemeyecek.

---

# Aşama 10 - Endpointleri Koru YAPILDI

Örneğin

```
POST /dokum
```

Artık sadece giriş yapan kullanıcı kullanabilecek.

Login olmayan kullanıcı

```
401 Unauthorized
```

alacak.

---

# Test Senaryosu (Postman)

## Kullanıcı Oluştur

↓

## Login

↓

Response

```
Access Token

Refresh Token
```

↓

Access Token'ı kopyala.

↓

Postman

Authorization

↓

Bearer Token

↓

Access Token'ı manuel olarak yapıştır.

↓

Korunan endpointi çağır.

```
POST /dokum
```

↓

```
200 OK
```

↓

Access Token'ın süresi dolsun.

↓

```
POST /auth/refresh
```

↓

Yeni Access Token dönsün.

↓

Postman'de eski Bearer Token'ı sil.

↓

Yeni Access Token'ı manuel olarak yapıştır.

↓

Tekrar

```
POST /dokum
```

↓

```
200 OK
```

↓

Logout

↓

```
POST /auth/logout
```

↓

Aynı Refresh Token ile tekrar

```
POST /auth/refresh
```

↓

```
401 Unauthorized
```

veya

```
Refresh Token aktif değil.
```

---

# Paket Yapısı

```
security
│
├── SecurityConfig
├── JwtService
├── JwtFilter
├── JwtAuthenticationEntryPoint
└── JwtProperties

auth
│
├── AuthController
├── AuthService
├── LoginRequest
├── LoginResponse
└── RefreshRequest

token
│
├── RefreshToken
├── RefreshTokenRepository
└── RefreshTokenService

user
│
├── User
├── UserRepository
└── UserService
```

---

# Claude Code ile Çalışma Sırası

Her adımı ayrı prompt olarak yaptır.

## Prompt 1

Spring Security dependency ekle ve SecurityConfig oluştur.

Şimdilik bütün endpointler `permitAll()` olsun.

---

## Prompt 2

JWT Service oluştur.

Token üretebilsin ve doğrulayabilsin.

---

## Prompt 3

Login endpointini oluştur.

Henüz sadece Access Token dönsün.

---

## Prompt 4

JWT Filter yaz.

Authorization Bearer Token ile gelen isteği doğrulasın.

---

## Prompt 5

RefreshToken Entity, Repository ve Service katmanını oluştur.

---

## Prompt 6

Login sırasında Refresh Token üret ve veritabanına kaydet.

---

## Prompt 7

`/auth/refresh` endpointini yaz.

---

## Prompt 8

`/auth/logout` endpointini yaz.

---

## Prompt 9

Döküm ve malzeme endpointlerini authentication zorunlu olacak şekilde güncelle.

---

# Hedef

Proje sonunda aşağıdaki akış çalışıyor olmalı.

```
Login
      ↓
Access Token + Refresh Token
      ↓
(Postman'de Access Token manuel olarak Bearer Token alanına eklenir)
      ↓
Korunan Endpoint
      ↓
Access Token Süresi Doldu
      ↓
Refresh Endpoint
      ↓
Yeni Access Token
      ↓
(Postman'de eski Bearer Token silinir ve yenisi manuel eklenir)
      ↓
İşleme Devam
```