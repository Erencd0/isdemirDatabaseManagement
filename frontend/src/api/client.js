// Central API layer. Every request made from Dashboard/Login goes through here.
//
// What it does:
//  - Keeps the base URL and the JSON headers in one place
//  - Adds the access token from localStorage to every request as "Authorization: Bearer ..."
//  - On a 401 it silently renews the access token through /auth/refresh and repeats the
//    request; the user notices nothing. Only when the refresh token is dead/revoked does
//    the session drop and the user go back to /login.

// Bos: istekler sayfanin kendi adresine gider ("/api/dokum" -> localhost:3000/api/dokum).
// Oradan backend'e iletme isini dev'de Vite proxy'si, Docker'da nginx yapiyor
// (vite.config.js ve frontend/nginx.conf). Adres sabit yazilmadigi icin uygulama
// hangi makinede/portta acilirsa acilsin backend'i bulur, CORS da devreye girmez.
const BASE_URL = ''

// The localStorage keys live in one place so they cannot be mistyped.
// The key strings stay as they are -- changing them would drop existing sessions.
export const STORAGE_KEYS = {
  userId: 'kullanici_id',
  username: 'kullanici_adi',
  selectedRole: 'secili_rol',
  accessToken: 'access_token',
  refreshToken: 'refresh_token',
}

// Flag that carries "the session was dropped because of a 401" over to the login page.
// sessionStorage: still readable after a full page redirect, cleared when the browser closes.
const SESSION_DROPPED = 'oturum_dustu'

export const getAccessToken = () => localStorage.getItem(STORAGE_KEYS.accessToken)
export const getRefreshToken = () => localStorage.getItem(STORAGE_KEYS.refreshToken)

// Reaching the protected pages needs both a token and a selected role
export const hasSession = () =>
  Boolean(getAccessToken() && localStorage.getItem(STORAGE_KEYS.selectedRole))

// Called once the login is complete (password verified + role selected)
export function saveSession({ user, selectedRole }) {
  localStorage.setItem(STORAGE_KEYS.userId, String(user.kullanici_id))
  localStorage.setItem(STORAGE_KEYS.username, user.kullanici_adi)
  localStorage.setItem(STORAGE_KEYS.selectedRole, selectedRole)
  localStorage.setItem(STORAGE_KEYS.accessToken, user.accessToken)
  localStorage.setItem(STORAGE_KEYS.refreshToken, user.refreshToken)
}

// Full logout: everything is removed, the user information included
export function clearSession() {
  Object.values(STORAGE_KEYS).forEach((k) => localStorage.removeItem(k))
  sessionStorage.removeItem(SESSION_DROPPED)
}

// The login page reads this: did we land here because of a 401? (reading clears the flag)
export function wasSessionDropped() {
  const dropped = sessionStorage.getItem(SESSION_DROPPED) === '1'
  sessionStorage.removeItem(SESSION_DROPPED)
  return dropped
}

// Called only when the refresh has ALSO failed: the session is really over, everything goes.
function dropSession() {
  clearSession()
  sessionStorage.setItem(SESSION_DROPPED, '1')
  // We are outside React Router here (this file is not a component), so the browser
  // performs the redirect.
  if (window.location.pathname !== '/login') {
    window.location.replace('/login')
  }
}

// Thrown after a 401. The caller should catch it and NOT show a "server error" message;
// we are already being redirected to the login page.
export class SessionError extends Error {
  constructor() {
    super('Oturum sona erdi')
    this.name = 'SessionError'
  }
}

// The only way to call the protected endpoints.
//  path   : e.g. "/api/dokum" (BASE_URL is prepended)
//  options: fetch options; `body` is given as a PLAIN OBJECT and turned into JSON here.
//           A FormData body (photo upload) is passed through untouched - the browser has to
//           set its own multipart Content-Type with the boundary, so we must not set one.
export async function apiFetch(path, options = {}) {
  const { body, headers, ...rest } = options
  const isFormData = body instanceof FormData

  // Reads the token at call time, so the retry automatically uses the fresh one.
  const send = () => {
    const requestHeaders = { ...headers }
    const token = getAccessToken()
    if (token) requestHeaders.Authorization = `Bearer ${token}`
    if (body !== undefined && !isFormData) requestHeaders['Content-Type'] = 'application/json'

    return fetch(`${BASE_URL}${path}`, {
      ...rest,
      headers: requestHeaders,
      body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body),
    })
  }

  let res = await send()

  // Expired access token: renew it in the background and repeat the request once.
  if (res.status === 401 && (await refreshAccessToken())) {
    res = await send()
  }

  if (res.status === 401) {
    dropSession()
    throw new SessionError()
  }
  return res
}

// POST /auth/login - verifies username + password (the session is not opened yet, a role
// still has to be selected). The response itself is returned so Login can inspect the
// status codes.
export function loginRequest(username, password) {
  return fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ kullanici_adi: username, kullanici_parola: password }),
  })
}

// POST /auth/refresh - gets a new access token with the stored refresh token.
// Stores the new token and returns true on success, false otherwise.
//
// Single flight: when several requests get a 401 at the same time they all wait for the SAME
// refresh call instead of firing one each.
let refreshInFlight = null

export function refreshAccessToken() {
  if (!refreshInFlight) {
    refreshInFlight = doRefresh().finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

async function doRefresh() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false

  try {
    const res = await fetch(`${BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
    if (!res.ok) return false

    const { accessToken } = await res.json()
    if (!accessToken) return false

    localStorage.setItem(STORAGE_KEYS.accessToken, accessToken)
    return true
  } catch {
    return false
  }
}

// POST /auth/logout - deactivates the refresh token in the DB (aktif = false).
// The local session is cleared in any case, even when the server cannot be reached.
export async function logoutRequest() {
  const refreshToken = getRefreshToken()
  if (refreshToken) {
    try {
      await fetch(`${BASE_URL}/auth/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      })
    } catch {
      // ignore silently; the local session is cleared anyway
    }
  }
  clearSession()
}
