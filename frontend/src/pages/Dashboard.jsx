import { useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import Button from '../components/Button.jsx'
import Modal from '../components/Modal.jsx'

const API = 'http://localhost:8080/api'

// Dokum ekleme formundaki alanlar. malzeme_kodu haric (kural geregi),
// dokum_id / dokum_no / kayit_zamani / kullanici_id backend'de otomatik atanir.
// Alan adlari backend JSON'i ile ayni (camelCase) olmali.
const DOKUM_ALANLARI = [
  { name: 'hurdaSarjBaslamaZamani', label: 'Hurda Şarj Başlama', type: 'datetime-local' },
  { name: 'hurdaSarjBitisZamani', label: 'Hurda Şarj Bitiş', type: 'datetime-local' },
  { name: 'anaUflemeyeBaslamaZamani', label: 'Ana Üflemeye Başlama', type: 'datetime-local' },
  { name: 'anaUflemeBitisZamani', label: 'Ana Üfleme Bitiş', type: 'datetime-local' },
  { name: 'dokumZamani', label: 'Döküm Zamanı', type: 'datetime-local' },
  { name: 'shdSicaklik', label: 'SHD Sıcaklık', type: 'number', min: 1200, max: 1500 },
  { name: 'dokumSicaklik', label: 'Döküm Sıcaklık', type: 'number', min: 1500, max: 1700 },
  {
    name: 'lansSkalDurum',
    label: 'Lans Skal Durum',
    type: 'select',
    required: true,
    // Gorunen metin AZ SKALLI, DB'ye giden deger AZ_SKALLI (DB sadece bu 3'unu kabul eder)
    options: [
      { value: 'AZ_SKALLI', label: 'AZ SKALLI' },
      { value: 'ORTA_SKALLI', label: 'ORTA SKALLI' },
      { value: 'COK_SKALLI', label: 'ÇOK SKALLI' },
    ],
  },
]

const BOS_FORM = DOKUM_ALANLARI.reduce((acc, a) => ({ ...acc, [a.name]: '' }), {})

// Malzeme ekleme (4-5-6). 3 katki butonu, DB'deki malzeme_turu degerlerine eslenir.
// Butona basilinca o ture ait malzemeler /api/malzeme?tur=... ile combobox'a doldurulur.
const KATKILAR = [
  { tur: 'KONVKATKI', label: 'Konverter Katkı' },
  { tur: 'POTAKATKI', label: 'Pota Katkı' },
  { tur: 'HURDAKATKI', label: 'Hurda Katkı' },
]

const BOS_MALZEME_FORM = { malzemeKodu: '', miktar: '', malzemeVerilisTarihi: '' }

// Katki turu kodu -> gorunen etiket (detay ve listede gostermek icin)
const KATKI_ETIKET = KATKILAR.reduce((acc, k) => ({ ...acc, [k.tur]: k.label }), {})

// Alan adi -> gorunen etiket (zaman dogrulama mesajlarinda kullanilir)
const ALAN_ETIKET = DOKUM_ALANLARI.reduce((acc, a) => ({ ...acc, [a.name]: a.label }), {})

// Islem zamanlarinin kronolojik sirasi. Her zaman bir oncekinden ONCE olamaz:
// hurda sarj basla -> bitis -> ana uflemeye basla -> ana ufleme bitis -> dokum
const ZAMAN_SIRASI = [
  'hurdaSarjBaslamaZamani',
  'hurdaSarjBitisZamani',
  'anaUflemeyeBaslamaZamani',
  'anaUflemeBitisZamani',
  'dokumZamani',
]

// datetime-local degerini okunabilir hale getirir ("2026-08-02T14:30" -> "02.08.2026 14:30")
function tarihGoster(deger) {
  if (!deger) return '-'
  const d = new Date(deger)
  if (isNaN(d)) return deger
  return d.toLocaleString('tr-TR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function Dashboard() {
  const navigate = useNavigate()

  const kullaniciAdi = localStorage.getItem('kullanici_adi')
  const seciliRol = localStorage.getItem('secili_rol')
  const kullaniciId = localStorage.getItem('kullanici_id')

  // Secilen rol (kv1/kv2/kv3) -> konverter no (1/2/3). dokum_no'nun 2. hanesi budur.
  const konverterNo = Number((seciliRol || '').replace(/[^0-9]/g, ''))

  // Dokum ekleme formu
  const [form, setForm] = useState(BOS_FORM)
  const [kaydediliyor, setKaydediliyor] = useState(false)
  const [formMesaj, setFormMesaj] = useState(null) // sadece kayit sonucu (basari/sunucu hatasi)
  const [hatalar, setHatalar] = useState({}) // alan bazli dogrulama hatalari: { alanAdi: mesaj }

  // Kaydedilmis dokum (varsa). Kaydet sonrasi burada tutulur; malzemeler bu dokume
  // eklenir. Doluysa form kilitlenir (tekrar Kaydet duplike olusturmasin), "Yeni Dokum"
  // ile temizlenip sonraki dokume gecilir.
  const [kayitliDokum, setKayitliDokum] = useState(null)
  const kayitli = kayitliDokum !== null

  // Kaydedilince bu kullanicinin alacagi dokum_no (backend hesaplar, degistirilemez gosterilir)
  const [sonrakiNo, setSonrakiNo] = useState(null)

  // Dokumleri listele popup'i
  const [listeAcik, setListeAcik] = useState(false)
  const [dokumler, setDokumler] = useState([])
  const [listeYukleniyor, setListeYukleniyor] = useState(false)
  const [listeHata, setListeHata] = useState('')

  // Detay popup'i
  const [detay, setDetay] = useState(null) // { dokum, malzemeler }
  const [detayYukleniyor, setDetayYukleniyor] = useState(false)

  // --- Malzeme ekleme (4-5-6) ---
  // Secili katki (KONVKATKI/POTAKATKI/HURDAKATKI). Bir butona basilmadan malzeme formu acilmaz.
  const [seciliKatki, setSeciliKatki] = useState(null)
  const [malzemeSecenekleri, setMalzemeSecenekleri] = useState([]) // secili katkinin comboboxu
  const [malzemeYukleniyor, setMalzemeYukleniyor] = useState(false)
  const [malzemeForm, setMalzemeForm] = useState(BOS_MALZEME_FORM)
  const [malzemeHatalar, setMalzemeHatalar] = useState({})
  const [malzemeMesaj, setMalzemeMesaj] = useState(null)
  const [malzemeKaydediliyor, setMalzemeKaydediliyor] = useState(false)
  // Bu dokume eklenmis malzemeler (kayit sonrasi listede gosterilir). Satirlarda
  // ayrica _tur ve malzemeAdi (client tarafinda) tutulur ki listede/duzenlemede kullanilsin.
  const [eklenenMalzemeler, setEklenenMalzemeler] = useState([])
  // Duzenlenen satir (varsa duzenleme modali acilir)
  const [duzenlenen, setDuzenlenen] = useState(null)

  // Bu kullanicinin alacagi siradaki dokum_no'yu backend'den getirir
  const sonrakiNoGetir = async () => {
    try {
      const res = await fetch(`${API}/dokum/sonraki-no?konverterNo=${konverterNo}`)
      if (res.ok) setSonrakiNo(await res.json())
    } catch {
      // sessiz gec, numara gosterilemezse form yine calisir
    }
  }

  // Acilista siradaki dokum_no'yu getir
  useEffect(() => {
    sonrakiNoGetir()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Oturum yoksa login'e don
  if (!kullaniciAdi || !seciliRol) {
    return <Navigate to="/login" replace />
  }

  const cikisYap = () => {
    localStorage.removeItem('kullanici_id')
    localStorage.removeItem('kullanici_adi')
    localStorage.removeItem('secili_rol')
    navigate('/login')
  }

  const alanDegistir = (name, value) => {
    setForm((onceki) => ({ ...onceki, [name]: value }))
    // Kullanici alani duzeltirken o alanin hatasini kaldir
    setHatalar((h) => {
      if (!h[name]) return h
      const kalan = { ...h }
      delete kalan[name]
      return kalan
    })
  }

  // Mevcut dokumu tamamen bitirir, sonraki dokume gecer: formu temizler,
  // kayitli dokumu birakir ve siradaki dokum_no'yu tazeler.
  const yeniDokum = () => {
    setForm(BOS_FORM)
    setFormMesaj(null)
    setHatalar({})
    setKayitliDokum(null)
    // Malzeme bolumunu de sifirla: yeni dokum, yeni malzeme listesi
    setSeciliKatki(null)
    setMalzemeSecenekleri([])
    setMalzemeForm(BOS_MALZEME_FORM)
    setMalzemeHatalar({})
    setMalzemeMesaj(null)
    setEklenenMalzemeler([])
    sonrakiNoGetir()
  }

  // 4. ADIM: bir katki butonuna basilinca o ture ait malzemeleri combobox'a yukle
  const katkiSec = async (tur) => {
    setSeciliKatki(tur)
    setMalzemeForm((f) => ({ ...f, malzemeKodu: '' })) // katki degisince secim sifirlanir
    setMalzemeHatalar({})
    setMalzemeMesaj(null)
    setMalzemeYukleniyor(true)
    try {
      const res = await fetch(`${API}/malzeme?tur=${tur}`)
      setMalzemeSecenekleri(res.ok ? await res.json() : [])
    } catch {
      setMalzemeSecenekleri([])
    } finally {
      setMalzemeYukleniyor(false)
    }
  }

  const malzemeAlanDegistir = (name, value) => {
    setMalzemeForm((f) => ({ ...f, [name]: value }))
    setMalzemeHatalar((h) => {
      if (!h[name]) return h
      const kalan = { ...h }
      delete kalan[name]
      return kalan
    })
  }

  // 5. ADIM: secili malzemeyi o anki dokume ekler ve alttaki listeye yansitir
  const malzemeEkle = async (e) => {
    e.preventDefault()
    if (!kayitli) return
    setMalzemeMesaj(null)

    const yeniHatalar = {}
    if (!malzemeForm.malzemeKodu) yeniHatalar.malzemeKodu = 'Malzeme seçiniz'
    if (malzemeForm.miktar === '') yeniHatalar.miktar = 'Miktar giriniz'
    else if (Number(malzemeForm.miktar) <= 0) yeniHatalar.miktar = "Miktar 0'dan büyük olmalı"
    if (!malzemeForm.malzemeVerilisTarihi) yeniHatalar.malzemeVerilisTarihi = 'Tarih giriniz'
    if (Object.keys(yeniHatalar).length > 0) {
      setMalzemeHatalar(yeniHatalar)
      return
    }
    setMalzemeHatalar({})

    setMalzemeKaydediliyor(true)
    try {
      const payload = {
        malzemeKodu: Number(malzemeForm.malzemeKodu),
        miktar: Number(malzemeForm.miktar),
        malzemeVerilisTarihi: malzemeForm.malzemeVerilisTarihi,
        kullaniciId: Number(kullaniciId),
      }
      const res = await fetch(`${API}/dokum/${kayitliDokum.dokumId}/malzeme`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      if (!res.ok) {
        // Backend dogrulama hatasi (400) mesajini goster; yoksa genel hata
        const metin = await res.text().catch(() => '')
        setMalzemeMesaj({ tip: 'hata', metin: metin || 'Malzeme eklenemedi' })
        return
      }
      const kaydedilen = await res.json()
      // Backend malzeme_adi doldurmaz; secili combobox'tan adi ve turu satira ekliyoruz
      const ad = malzemeSecenekleri.find((m) => m.malzemeKodu === payload.malzemeKodu)?.malzemeAdi
      setEklenenMalzemeler((prev) => [...prev, { ...kaydedilen, malzemeAdi: ad, _tur: seciliKatki }])
      // Miktar ve tarih temizlenir, katki secili kalir (art arda ekleme kolay olsun)
      setMalzemeForm((f) => ({ ...f, malzemeKodu: '', miktar: '', malzemeVerilisTarihi: '' }))
      setMalzemeMesaj({ tip: 'ok', metin: 'Malzeme eklendi' })
    } catch {
      setMalzemeMesaj({ tip: 'hata', metin: 'Sunucuya bağlanılamadı' })
    } finally {
      setMalzemeKaydediliyor(false)
    }
  }

  // 6. ADIM: listeden bir malzemeyi siler
  const malzemeSil = async (kullanimId) => {
    try {
      const res = await fetch(`${API}/dokum/${kayitliDokum.dokumId}/malzeme/${kullanimId}`, {
        method: 'DELETE',
      })
      if (res.ok) {
        setEklenenMalzemeler((prev) => prev.filter((m) => m.kullanimId !== kullanimId))
      }
    } catch {
      // sessiz gec; silinemezse liste degismez
    }
  }

  // Duzenleme modali kaydedince listedeki satiri gunceller
  const malzemeGuncellendi = (guncel) => {
    setEklenenMalzemeler((prev) =>
      prev.map((m) => (m.kullanimId === guncel.kullanimId ? guncel : m)),
    )
    setDuzenlenen(null)
  }

  const dokumKaydet = async (e) => {
    e.preventDefault()
    // Zaten kaydedildiyse tekrar kaydetme (backend yeni kayit olusturur, duplike olur)
    if (kayitli) return
    setFormMesaj(null)

    // Alan bazli dogrulama: bos olamaz + sicaklik araligi. Hatalar her alanin altinda gosterilir.
    const yeniHatalar = {}
    for (const alan of DOKUM_ALANLARI) {
      const deger = form[alan.name]
      if (deger === '' || deger == null) {
        yeniHatalar[alan.name] = 'Bu alan boş bırakılamaz'
        continue
      }
      if (alan.type === 'number' && alan.min != null) {
        const sayi = Number(deger)
        if (sayi < alan.min || sayi > alan.max) {
          yeniHatalar[alan.name] = `${alan.min}-${alan.max} aralığında olmalı`
        }
      }
    }

    // Zaman siralamasi: her zaman kendinden onceki islem zamanindan once olamaz.
    // Bos alanlar zaten yukarida hata aldigi icin burada atlanir.
    for (let i = 1; i < ZAMAN_SIRASI.length; i++) {
      const oncekiAd = ZAMAN_SIRASI[i - 1]
      const suankiAd = ZAMAN_SIRASI[i]
      const onceki = form[oncekiAd]
      const suanki = form[suankiAd]
      if (onceki && suanki && !yeniHatalar[suankiAd] && new Date(suanki) < new Date(onceki)) {
        yeniHatalar[suankiAd] = `${ALAN_ETIKET[oncekiAd]} zamanından önce olamaz`
      }
    }

    if (Object.keys(yeniHatalar).length > 0) {
      setHatalar(yeniHatalar)
      return
    }
    setHatalar({})

    setKaydediliyor(true)
    try {
      // Bos alanlar null gonderilir, sayilar Number'a cevrilir.
      // konverterNo secilen rolden gelir (formda yok), dokum_no bununla uretilir.
      const payload = { kullaniciId: Number(kullaniciId), konverterNo }
      for (const alan of DOKUM_ALANLARI) {
        const deger = form[alan.name]
        if (deger === '' || deger == null) {
          payload[alan.name] = null
        } else if (alan.type === 'number') {
          payload[alan.name] = Number(deger)
        } else {
          payload[alan.name] = deger
        }
      }

      const res = await fetch(`${API}/dokum`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      if (!res.ok) {
        // Backend dogrulama hatasi (400) mesajini goster; yoksa genel hata
        const metin = await res.text().catch(() => '')
        setFormMesaj({
          tip: 'hata',
          metin: metin || 'Döküm kaydedilemedi (sunucu hatası)',
        })
        return
      }
      const kaydedilen = await res.json()
      // Dokum kaydedildi ama forma dokunulmaz ve numara ILERLEMEZ: bu dokume hala
      // malzeme eklenecek. Form kilitlenir; "Yeni Dokum" ile siradaki dokume gecilir.
      setKayitliDokum(kaydedilen)
      setFormMesaj({
        tip: 'ok',
        metin: `Döküm kaydedildi (No: ${kaydedilen.dokumNo ?? kaydedilen.dokumId}). Malzeme ekleyebilir veya "Yeni Döküm" ile sonraki döküme geçebilirsiniz.`,
      })
    } catch {
      setFormMesaj({ tip: 'hata', metin: 'Sunucuya bağlanılamadı' })
    } finally {
      setKaydediliyor(false)
    }
  }

  const dokumleriListele = async () => {
    setListeAcik(true)
    setListeHata('')
    setListeYukleniyor(true)
    try {
      const res = await fetch(`${API}/dokum`)
      if (!res.ok) {
        setListeHata('Dökümler alınamadı')
        return
      }
      setDokumler(await res.json())
    } catch {
      setListeHata('Sunucuya bağlanılamadı')
    } finally {
      setListeYukleniyor(false)
    }
  }

  const detayGor = async (id) => {
    setDetay(null)
    setDetayYukleniyor(true)
    try {
      const res = await fetch(`${API}/dokum/${id}`)
      if (!res.ok) return
      setDetay(await res.json())
    } catch {
      // sessiz gec, detay acilmaz
    } finally {
      setDetayYukleniyor(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 1. ADIM: Ust bar - giris yapan kisi + cikis */}
      <header className="bg-brand-700 text-white shadow">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-8 py-4">
          <h1 className="text-xl font-bold">İsdemir Döküm Paneli</h1>
          <div className="flex items-center gap-4">
            <div className="text-right leading-tight">
              <p className="font-semibold">{kullaniciAdi}</p>
              <p className="text-sm text-brand-100">Rol: {seciliRol}</p>
            </div>
            <Button variant="secondary" size="sm" onClick={cikisYap}>
              Çıkış Yap
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-8 py-6">
        {/* 2 + 3. ADIM: butonlar + her zaman gorunen dokum ekleme formu */}
        <div className="mb-4 flex gap-3">
          <Button onClick={dokumleriListele}>Dökümleri Listele</Button>
          <Button variant="outline" onClick={yeniDokum}>
            Yeni Döküm
          </Button>
        </div>

        <section className="rounded-2xl bg-white p-6 shadow">
          <h2 className="mb-4 text-lg font-bold text-brand-700">Yeni Döküm Ekle</h2>

          {/* Kaydedilecek dokum_no - degistirilemez, sadece bilgi amacli */}
          <div className="mb-4 flex items-center gap-3 rounded-lg bg-brand-50 px-4 py-3">
            <span className="text-sm font-medium text-gray-600">Döküm No:</span>
            <span className="text-lg font-bold text-brand-700">
              {kayitli ? kayitliDokum.dokumNo : (sonrakiNo ?? '...')}
            </span>
          </div>

          <form onSubmit={dokumKaydet}>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {DOKUM_ALANLARI.map((alan) => (
                <div key={alan.name}>
                  <label className="mb-1 block text-sm font-medium text-gray-700">
                    {alan.label}
                    <span className="ml-0.5 text-red-500">*</span>
                    {alan.min != null && (
                      <span className="ml-1 text-xs font-normal text-gray-400">
                        ({alan.min}-{alan.max})
                      </span>
                    )}
                  </label>
                  {alan.type === 'select' ? (
                    <select
                      value={form[alan.name]}
                      disabled={kayitli}
                      onChange={(e) => alanDegistir(alan.name, e.target.value)}
                      className={
                        'w-full rounded-lg border bg-white px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-gray-100 disabled:text-gray-500 ' +
                        (hatalar[alan.name] ? 'border-red-400' : 'border-gray-300')
                      }
                    >
                      <option value="">Seçiniz</option>
                      {alan.options.map((o) => (
                        <option key={o.value} value={o.value}>
                          {o.label}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <input
                      type={alan.type}
                      value={form[alan.name]}
                      min={alan.min}
                      max={alan.max}
                      disabled={kayitli}
                      onChange={(e) => alanDegistir(alan.name, e.target.value)}
                      className={
                        'w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-gray-100 disabled:text-gray-500 ' +
                        (hatalar[alan.name] ? 'border-red-400' : 'border-gray-300')
                      }
                    />
                  )}
                  {hatalar[alan.name] && (
                    <p className="mt-1 text-xs text-red-600">{hatalar[alan.name]}</p>
                  )}
                </div>
              ))}
            </div>

            {formMesaj && (
              <p
                className={
                  'mt-4 rounded-lg border px-3 py-2 text-sm ' +
                  (formMesaj.tip === 'ok'
                    ? 'border-green-200 bg-green-50 text-green-700'
                    : 'border-red-200 bg-red-50 text-red-600')
                }
              >
                {formMesaj.metin}
              </p>
            )}

            <div className="mt-4">
              <Button type="submit" disabled={kaydediliyor || kayitli}>
                {kaydediliyor ? 'Kaydediliyor...' : kayitli ? 'Kaydedildi ✓' : 'Kaydet'}
              </Button>
            </div>
          </form>
        </section>

        {/* 4-5-6. ADIM: Malzeme ekleme bolumu (dokum kaydedildikten sonra aktif) */}
        <section className="mt-6 rounded-2xl bg-white p-6 shadow">
          <h2 className="mb-1 text-lg font-bold text-brand-700">Malzeme Ekle</h2>
          {!kayitli && (
            <p className="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">
              Malzeme ekleyebilmek için önce yukarıdan dökümü kaydedin.
            </p>
          )}

          {/* 4. ADIM: katki butonlari - once katki secilir, malzemeler ona gore gelir */}
          <div className="mb-4 flex flex-wrap gap-3">
            {KATKILAR.map((k) => (
              <Button
                key={k.tur}
                variant={seciliKatki === k.tur ? 'primary' : 'outline'}
                disabled={!kayitli}
                onClick={() => katkiSec(k.tur)}
              >
                {k.label}
              </Button>
            ))}
          </div>

          {/* 5. ADIM: secili katkiya ait malzeme bilgileri girilir */}
          {kayitli && seciliKatki && (
            <form onSubmit={malzemeEkle} className="mb-6">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                <div className="sm:col-span-3">
                  <label className="mb-1 block text-sm font-medium text-gray-700">
                    Malzeme<span className="ml-0.5 text-red-500">*</span>
                  </label>
                  <select
                    value={malzemeForm.malzemeKodu}
                    disabled={malzemeYukleniyor}
                    onChange={(e) => malzemeAlanDegistir('malzemeKodu', e.target.value)}
                    className={
                      'w-full rounded-lg border bg-white px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-gray-100 ' +
                      (malzemeHatalar.malzemeKodu ? 'border-red-400' : 'border-gray-300')
                    }
                  >
                    <option value="">{malzemeYukleniyor ? 'Yükleniyor...' : 'Seçiniz'}</option>
                    {malzemeSecenekleri.map((m) => (
                      <option key={m.malzemeKodu} value={m.malzemeKodu}>
                        {m.malzemeKodu} - {m.malzemeAdi}
                      </option>
                    ))}
                  </select>
                  {malzemeHatalar.malzemeKodu && (
                    <p className="mt-1 text-xs text-red-600">{malzemeHatalar.malzemeKodu}</p>
                  )}
                </div>

                <div>
                  <label className="mb-1 block text-sm font-medium text-gray-700">
                    Miktar (kg)<span className="ml-0.5 text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    min={1}
                    value={malzemeForm.miktar}
                    onChange={(e) => malzemeAlanDegistir('miktar', e.target.value)}
                    className={
                      'w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-500 ' +
                      (malzemeHatalar.miktar ? 'border-red-400' : 'border-gray-300')
                    }
                  />
                  {malzemeHatalar.miktar && (
                    <p className="mt-1 text-xs text-red-600">{malzemeHatalar.miktar}</p>
                  )}
                </div>

                <div className="sm:col-span-2">
                  <label className="mb-1 block text-sm font-medium text-gray-700">
                    Veriliş Tarihi<span className="ml-0.5 text-red-500">*</span>
                  </label>
                  <input
                    type="datetime-local"
                    value={malzemeForm.malzemeVerilisTarihi}
                    onChange={(e) => malzemeAlanDegistir('malzemeVerilisTarihi', e.target.value)}
                    className={
                      'w-full rounded-lg border px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-500 ' +
                      (malzemeHatalar.malzemeVerilisTarihi ? 'border-red-400' : 'border-gray-300')
                    }
                  />
                  {malzemeHatalar.malzemeVerilisTarihi && (
                    <p className="mt-1 text-xs text-red-600">{malzemeHatalar.malzemeVerilisTarihi}</p>
                  )}
                </div>
              </div>

              {malzemeMesaj && (
                <p
                  className={
                    'mt-4 rounded-lg border px-3 py-2 text-sm ' +
                    (malzemeMesaj.tip === 'ok'
                      ? 'border-green-200 bg-green-50 text-green-700'
                      : 'border-red-200 bg-red-50 text-red-600')
                  }
                >
                  {malzemeMesaj.metin}
                </p>
              )}

              <div className="mt-4">
                <Button type="submit" disabled={malzemeKaydediliyor}>
                  {malzemeKaydediliyor ? 'Ekleniyor...' : 'Malzeme Ekle'}
                </Button>
              </div>
            </form>
          )}

          {/* 5-6. ADIM: bu dokume eklenen malzemeler listesi + sil/guncelle */}
          <div>
            <h3 className="mb-2 text-sm font-bold text-gray-700">
              Eklenen Malzemeler ({eklenenMalzemeler.length})
            </h3>
            {eklenenMalzemeler.length === 0 ? (
              <p className="text-sm text-gray-500">Henüz malzeme eklenmedi.</p>
            ) : (
              <table className="w-full text-left text-sm">
                <thead className="text-gray-500">
                  <tr className="border-b border-gray-200">
                    <th className="py-2">Katkı</th>
                    <th className="py-2">Kod</th>
                    <th className="py-2">Malzeme Adı</th>
                    <th className="py-2">Miktar (kg)</th>
                    <th className="py-2">Veriliş Tarihi</th>
                    <th className="py-2 text-right">İşlem</th>
                  </tr>
                </thead>
                <tbody>
                  {eklenenMalzemeler.map((m) =>
                    duzenlenen?.kullanimId === m.kullanimId ? (
                      <MalzemeSatirDuzenle
                        key={m.kullanimId}
                        satir={m}
                        dokumId={kayitliDokum.dokumId}
                        kullaniciId={kullaniciId}
                        onKapat={() => setDuzenlenen(null)}
                        onKaydedildi={malzemeGuncellendi}
                      />
                    ) : (
                      <tr key={m.kullanimId} className="border-b border-gray-100">
                        <td className="py-2">{KATKI_ETIKET[m._tur] ?? m._tur ?? '-'}</td>
                        <td className="py-2">{m.malzemeKodu}</td>
                        <td className="py-2">{m.malzemeAdi ?? '-'}</td>
                        <td className="py-2">{m.miktar}</td>
                        <td className="py-2">{tarihGoster(m.malzemeVerilisTarihi)}</td>
                        <td className="py-2">
                          <div className="flex justify-end gap-2">
                            <Button size="sm" variant="outline" onClick={() => setDuzenlenen(m)}>
                              Güncelle
                            </Button>
                            <Button size="sm" variant="secondary" onClick={() => malzemeSil(m.kullanimId)}>
                              Sil
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ),
                  )}
                </tbody>
              </table>
            )}
          </div>
        </section>
      </main>

      {/* 3. ADIM: Dokumleri listele popup'i */}
      <Modal
        open={listeAcik}
        onClose={() => setListeAcik(false)}
        title="Dökümler"
        maxWidth="max-w-2xl"
      >
        {listeYukleniyor && <p className="text-gray-500">Yükleniyor...</p>}
        {listeHata && <p className="text-red-600">{listeHata}</p>}
        {!listeYukleniyor && !listeHata && dokumler.length === 0 && (
          <p className="text-gray-500">Kayıtlı döküm yok.</p>
        )}
        {!listeYukleniyor && dokumler.length > 0 && (
          <ul className="divide-y divide-gray-100">
            {dokumler.map((d) => (
              <li key={d.dokumId} className="flex items-center justify-between py-3">
                <div className="text-sm">
                  <p className="font-semibold text-gray-800">Döküm No: {d.dokumNo}</p>
                  <p className="text-gray-500">
                    {tarihGoster(d.dokumZamani)} · Sıcaklık: {d.dokumSicaklik ?? '-'}
                  </p>
                </div>
                <Button size="sm" variant="outline" onClick={() => detayGor(d.dokumId)}>
                  Detay Gör
                </Button>
              </li>
            ))}
          </ul>
        )}
      </Modal>

      {/* 3. ADIM: Detay popup'i (listenin ustune biner) */}
      <Modal
        open={detay !== null || detayYukleniyor}
        onClose={() => setDetay(null)}
        title={detay ? `Döküm No: ${detay.dokum.dokumNo}` : 'Detay'}
        maxWidth="max-w-2xl"
      >
        {detayYukleniyor && <p className="text-gray-500">Yükleniyor...</p>}
        {detay && (
          <div className="space-y-5">
            <div>
              <h3 className="mb-2 text-sm font-bold text-brand-700">Döküm Bilgileri</h3>
              <dl className="grid grid-cols-1 gap-x-6 gap-y-1 text-sm sm:grid-cols-2">
                <DetaySatir etiket="Döküm ID" deger={detay.dokum.dokumId} />
                <DetaySatir etiket="Döküm No" deger={detay.dokum.dokumNo} />
                <DetaySatir etiket="Hurda Şarj Başlama" deger={tarihGoster(detay.dokum.hurdaSarjBaslamaZamani)} />
                <DetaySatir etiket="Hurda Şarj Bitiş" deger={tarihGoster(detay.dokum.hurdaSarjBitisZamani)} />
                <DetaySatir etiket="Ana Üflemeye Başlama" deger={tarihGoster(detay.dokum.anaUflemeyeBaslamaZamani)} />
                <DetaySatir etiket="Ana Üfleme Bitiş" deger={tarihGoster(detay.dokum.anaUflemeBitisZamani)} />
                <DetaySatir etiket="Döküm Zamanı" deger={tarihGoster(detay.dokum.dokumZamani)} />
                <DetaySatir etiket="SHD Sıcaklık" deger={detay.dokum.shdSicaklik} />
                <DetaySatir etiket="Döküm Sıcaklık" deger={detay.dokum.dokumSicaklik} />
                <DetaySatir etiket="Lans Skal Durum" deger={detay.dokum.lansSkalDurum} />
                <DetaySatir etiket="Kayıt Zamanı" deger={tarihGoster(detay.dokum.kayitZamani)} />
                <DetaySatir etiket="Kullanıcı ID" deger={detay.dokum.kullaniciId} />
              </dl>
            </div>

            <div>
              <h3 className="mb-2 text-sm font-bold text-brand-700">
                Malzemeler ({detay.malzemeler.length})
              </h3>
              {detay.malzemeler.length === 0 ? (
                <p className="text-sm text-gray-500">Bu döküme eklenmiş malzeme yok.</p>
              ) : (
                <table className="w-full text-left text-sm">
                  <thead className="text-gray-500">
                    <tr className="border-b border-gray-200">
                      <th className="py-2">Katkı</th>
                      <th className="py-2">Malzeme Kodu</th>
                      <th className="py-2">Malzeme Adı</th>
                      <th className="py-2">Miktar (kg)</th>
                      <th className="py-2">Veriliş Tarihi</th>
                    </tr>
                  </thead>
                  <tbody>
                    {detay.malzemeler.map((m) => (
                      <tr key={m.kullanimId} className="border-b border-gray-100">
                        <td className="py-2">{KATKI_ETIKET[m.malzemeTuru] ?? m.malzemeTuru ?? '-'}</td>
                        <td className="py-2">{m.malzemeKodu}</td>
                        <td className="py-2">{m.malzemeAdi ?? '-'}</td>
                        <td className="py-2">{m.miktar}</td>
                        <td className="py-2">{tarihGoster(m.malzemeVerilisTarihi)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}

// 6. ADIM: Malzeme guncelleme - listedeki satirin KENDI uzerinde (inline) yapilir.
// Duzenlenen satir bir <tr> olarak render edilir; hucreler input/select'e donusur.
// Satirin ait oldugu katkinin (_tur) malzemelerini yukler, mevcut degerleri onceden
// doldurur, PUT ile gunceller.
function MalzemeSatirDuzenle({ satir, dokumId, kullaniciId, onKapat, onKaydedildi }) {
  const [tur, setTur] = useState(satir._tur ?? '')
  const [secenekler, setSecenekler] = useState([])
  const [yukleniyor, setYukleniyor] = useState(false)
  const [form, setForm] = useState({
    malzemeKodu: String(satir.malzemeKodu ?? ''),
    miktar: String(satir.miktar ?? ''),
    // datetime-local "YYYY-MM-DDTHH:mm" bekler; backend'den gelen ISO'nun ilk 16 karakteri yeterli
    malzemeVerilisTarihi: satir.malzemeVerilisTarihi
      ? satir.malzemeVerilisTarihi.slice(0, 16)
      : '',
  })
  const [kaydediliyor, setKaydediliyor] = useState(false)
  const [hata, setHata] = useState('')

  // Katki degisince o katkinin malzemelerini combobox'a yukle (acilista da calisir)
  useEffect(() => {
    if (!tur) return
    let iptal = false
    setYukleniyor(true)
    fetch(`${API}/malzeme?tur=${tur}`)
      .then((r) => (r.ok ? r.json() : []))
      .then((d) => {
        if (!iptal) setSecenekler(d)
      })
      .catch(() => {
        if (!iptal) setSecenekler([])
      })
      .finally(() => {
        if (!iptal) setYukleniyor(false)
      })
    return () => {
      iptal = true
    }
  }, [tur])

  const kaydet = async () => {
    setHata('')
    if (!form.malzemeKodu || form.miktar === '' || !form.malzemeVerilisTarihi) {
      setHata('Tüm alanları doldurun')
      return
    }
    if (Number(form.miktar) <= 0) {
      setHata("Miktar 0'dan büyük olmalı")
      return
    }
    setKaydediliyor(true)
    try {
      const payload = {
        malzemeKodu: Number(form.malzemeKodu),
        miktar: Number(form.miktar),
        malzemeVerilisTarihi: form.malzemeVerilisTarihi,
        kullaniciId: Number(kullaniciId),
      }
      const res = await fetch(`${API}/dokum/${dokumId}/malzeme/${satir.kullanimId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      if (!res.ok) {
        // Backend dogrulama hatasi (400) mesajini goster; yoksa genel hata
        const metin = await res.text().catch(() => '')
        setHata(metin || 'Güncellenemedi')
        return
      }
      const guncel = await res.json()
      const ad =
        secenekler.find((m) => m.malzemeKodu === payload.malzemeKodu)?.malzemeAdi ??
        satir.malzemeAdi
      onKaydedildi({ ...guncel, malzemeAdi: ad, _tur: tur })
    } catch {
      setHata('Sunucuya bağlanılamadı')
    } finally {
      setKaydediliyor(false)
    }
  }

  // Malzeme Adi hucresinde secili malzemenin adi gosterilir (combobox degistikce guncellenir)
  const seciliAd =
    secenekler.find((m) => String(m.malzemeKodu) === form.malzemeKodu)?.malzemeAdi ??
    satir.malzemeAdi ??
    '-'

  const hucreInput =
    'w-full rounded-lg border border-gray-300 bg-white px-2 py-1 focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-gray-100'

  return (
    <>
      <tr className="border-b border-gray-100 bg-brand-50/50 align-top">
        <td className="py-2 pr-2">
          <select
            value={tur}
            onChange={(e) => {
              setTur(e.target.value)
              setForm((f) => ({ ...f, malzemeKodu: '' }))
            }}
            className={hucreInput}
          >
            {KATKILAR.map((k) => (
              <option key={k.tur} value={k.tur}>
                {k.label}
              </option>
            ))}
          </select>
        </td>
        <td className="py-2 pr-2">
          <select
            value={form.malzemeKodu}
            disabled={yukleniyor}
            onChange={(e) => setForm((f) => ({ ...f, malzemeKodu: e.target.value }))}
            className={hucreInput}
          >
            <option value="">{yukleniyor ? 'Yükleniyor...' : 'Seçiniz'}</option>
            {secenekler.map((m) => (
              <option key={m.malzemeKodu} value={m.malzemeKodu}>
                {m.malzemeKodu} - {m.malzemeAdi}
              </option>
            ))}
          </select>
        </td>
        <td className="py-2 pr-2 text-gray-500">{seciliAd}</td>
        <td className="py-2 pr-2">
          <input
            type="number"
            min={1}
            value={form.miktar}
            onChange={(e) => setForm((f) => ({ ...f, miktar: e.target.value }))}
            className={hucreInput}
          />
        </td>
        <td className="py-2 pr-2">
          <input
            type="datetime-local"
            value={form.malzemeVerilisTarihi}
            onChange={(e) => setForm((f) => ({ ...f, malzemeVerilisTarihi: e.target.value }))}
            className={hucreInput}
          />
        </td>
        <td className="py-2">
          <div className="flex justify-end gap-2">
            <Button size="sm" onClick={kaydet} disabled={kaydediliyor}>
              {kaydediliyor ? '...' : 'Kaydet'}
            </Button>
            <Button size="sm" variant="outline" onClick={onKapat} disabled={kaydediliyor}>
              İptal
            </Button>
          </div>
        </td>
      </tr>
      {hata && (
        <tr>
          <td colSpan={6} className="pb-2 text-xs text-red-600">
            {hata}
          </td>
        </tr>
      )}
    </>
  )
}

// Detay popup'inda tek bir "etiket: deger" satiri
function DetaySatir({ etiket, deger }) {
  return (
    <div className="flex justify-between border-b border-gray-50 py-0.5">
      <dt className="text-gray-500">{etiket}</dt>
      <dd className="font-medium text-gray-800">{deger ?? '-'}</dd>
    </div>
  )
}

export default Dashboard
