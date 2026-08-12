import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Button from '../components/Button.jsx'
import { loginRequest, saveSession, wasSessionDropped } from '../api/client.js'

function Login() {
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [roles, setRoles] = useState([]) // filled once the login succeeds
  const [selectedRole, setSelectedRole] = useState('')
  const [user, setUser] = useState(null) // the successful login result
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const passwordVerified = user !== null // password checked, waiting for a role

  // An expired access token is renewed silently in client.js. We only land here when the
  // refresh token is dead too (expired / logged out) -- then the password is needed again.
  useEffect(() => {
    if (wasSessionDropped()) {
      setError('Oturumunuz sona erdi, lütfen tekrar giriş yapın')
    }
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    // Second phase: the password is verified, the login completes once the user picks a role
    if (passwordVerified) {
      if (!selectedRole) {
        setError('Lütfen bir rol seçin')
        return
      }
      // The access + refresh tokens are stored together with the user information;
      // requests to the protected endpoints use that access token.
      saveSession({ user, selectedRole })
      navigate('/dashboard')
      return
    }

    // First phase: send the username / password to the backend
    setLoading(true)
    try {
      const res = await loginRequest(username, password)

      if (res.status === 401) {
        setError('Kullanıcı adı veya parola hatalı')
        return
      }
      if (res.status === 403) {
        setError('Bu kullanıcının giriş yetkisi yok')
        return
      }
      if (!res.ok) {
        setError('Bir hata oluştu, lütfen tekrar deneyin')
        return
      }

      const data = await res.json()
      setUser(data)
      setRoles(data.roller || [])
      setSelectedRole('') // never auto selected, even with a single role
    } catch {
      setError('Sunucuya bağlanılamadı')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-brand-700 p-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-2xl p-8">
        <h1 className="text-3xl font-bold text-brand-700 text-center mb-1">İsdemir</h1>
        <p className="text-center text-gray-500 mb-6">
          Giriş Yap
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Kullanıcı Adı
            </label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={passwordVerified}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-gray-100 disabled:text-gray-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Parola
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={passwordVerified}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-gray-100 disabled:text-gray-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Rol
            </label>
            <select
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value)}
              disabled={!passwordVerified}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-gray-100 disabled:text-gray-400"
            >
              <option value="">
                {passwordVerified ? 'Rol seçin' : 'Giriş yapılmadı'}
              </option>
              {roles.map((role) => (
                <option key={role} value={role}>
                  {role}
                </option>
              ))}
            </select>
          </div>

          {error && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
              {error}
            </p>
          )}

          <Button type="submit" fullWidth disabled={loading}>
            {loading ? 'Kontrol ediliyor...' : passwordVerified ? 'Giriş' : 'Kontrol Et'}
          </Button>
        </form>
      </div>
    </div>
  )
}

export default Login
