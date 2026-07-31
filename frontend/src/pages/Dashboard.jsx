import { Navigate, useNavigate } from 'react-router-dom'
import Button from '../components/Button.jsx'

function Dashboard() {
  const navigate = useNavigate()

  const kullaniciAdi = localStorage.getItem('kullanici_adi')
  const seciliRol = localStorage.getItem('secili_rol')

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

  return (
    <div className="min-h-screen flex items-center justify-center bg-brand-700 p-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-2xl p-8 text-center">
        <h1 className="text-2xl font-bold text-brand-700 mb-2">Giriş Başarılı</h1>
        <p className="text-gray-700">
          Hoş geldin, <span className="font-semibold">{kullaniciAdi}</span>
        </p>
        <p className="text-gray-500 mt-1">
          Rol: <span className="font-semibold text-brand-700">{seciliRol}</span>
        </p>

        <Button onClick={cikisYap} fullWidth className="mt-6">
          Çıkış Yap
        </Button>
      </div>
    </div>
  )
}

export default Dashboard
