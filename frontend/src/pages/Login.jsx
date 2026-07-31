import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Button from '../components/Button.jsx'

const API_URL = 'http://localhost:8080/api/login'

function Login() {
  const navigate = useNavigate()

  const [kullaniciAdi, setKullaniciAdi] = useState('')
  const [parola, setParola] = useState('')
  const [roller, setRoller] = useState([]) // login basarili olunca dolar
  const [seciliRol, setSeciliRol] = useState('')
  const [kullanici, setKullanici] = useState(null) // basarili login sonucu
  const [hata, setHata] = useState('')
  const [yukleniyor, setYukleniyor] = useState(false)

  const girisYapildi = kullanici !== null // parola dogrulandi, rol bekleniyor

  const handleSubmit = async (e) => {
    e.preventDefault()
    setHata('')

    // Ikinci asama: parola dogrulandi, kullanici comboboxtan rol sectikten sonra giris tamamlanir
    if (girisYapildi) {
      if (!seciliRol) {
        setHata('Lütfen bir rol seçin')
        return
      }
      localStorage.setItem('kullanici_id', String(kullanici.kullanici_id))
      localStorage.setItem('kullanici_adi', kullanici.kullanici_adi)
      localStorage.setItem('secili_rol', seciliRol)
      navigate('/dashboard')
      return
    }

    // Ilk asama: kullanici adi / parola ile backend'e istek
    setYukleniyor(true)
    try {
      const res = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          kullanici_adi: kullaniciAdi,
          kullanici_parola: parola,
        }),
      })

      if (res.status === 401) {
        setHata('Kullanıcı adı veya parola hatalı')
        return
      }
      if (res.status === 403) {
        setHata('Bu kullanıcının giriş yetkisi yok')
        return
      }
      if (!res.ok) {
        setHata('Bir hata oluştu, lütfen tekrar deneyin')
        return
      }

      const data = await res.json()
      setKullanici(data)
      setRoller(data.roller || [])
      setSeciliRol('') // tek rol olsa bile otomatik secilmez
    } catch {
      setHata('Sunucuya bağlanılamadı')
    } finally {
      setYukleniyor(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-brand-700 p-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-2xl p-8">
        <h1 className="text-3xl font-bold text-brand-700 text-center mb-1">İsdemir</h1>
        <p className="text-center text-gray-500 mb-6">Giriş Yap</p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Kullanıcı Adı
            </label>
            <input
              type="text"
              value={kullaniciAdi}
              onChange={(e) => setKullaniciAdi(e.target.value)}
              disabled={girisYapildi}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-gray-100 disabled:text-gray-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Parola
            </label>
            <input
              type="password"
              value={parola}
              onChange={(e) => setParola(e.target.value)}
              disabled={girisYapildi}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-gray-100 disabled:text-gray-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Rol
            </label>
            <select
              value={seciliRol}
              onChange={(e) => setSeciliRol(e.target.value)}
              disabled={!girisYapildi}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-gray-100 disabled:text-gray-400"
            >
              <option value="">
                {girisYapildi ? 'Rol seçin' : 'Giriş yapılmadı'}
              </option>
              {roller.map((rol) => (
                <option key={rol} value={rol}>
                  {rol}
                </option>
              ))}
            </select>
          </div>

          {hata && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
              {hata}
            </p>
          )}

          <Button type="submit" fullWidth disabled={yukleniyor}>
            {yukleniyor ? 'Kontrol ediliyor...' : girisYapildi ? 'Giriş' : 'Kontrol Et'}
          </Button>
        </form>
      </div>
    </div>
  )
}

export default Login
