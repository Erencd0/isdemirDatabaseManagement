// Central API layer. Every request made from Dashboard/Login goes through here.
//
// What it does:
//  - Keeps the base URL and the JSON headers in one place
//  - Adds the access token from localStorage to every request as "Authorization: Bearer ..."
//  - On a 401 (missing / expired token) it drops the session and sends the user to /login
//
// Note: there is NO automatic (background) token refresh. On a 401 the user goes back to the
// login page; the refresh happens there through /auth/refresh (see refreshAccessToken).

const BASE_URL = 'http://localhost:8080'

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

// On a 401: the access token is no longer valid and gets removed. The refresh token and the
// user information STAY; the login page tries to renew the session with them.
function dropSession() {
  localStorage.removeItem(STORAGE_KEYS.accessToken)
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
//  options: fetch options; `body` is given as a PLAIN OBJECT and turned into JSON here
export async function apiFetch(path, options = {}) {
  const { body, headers, ...rest } = options

  const requestHeaders = { ...headers }
  const token = getAccessToken()
  if (token) requestHeaders.Authorization = `Bearer ${token}`
  if (body !== undefined) requestHeaders['Content-Type'] = 'application/json'

  const res = await fetch(`${BASE_URL}${path}`, {
    ...rest,
    headers: requestHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

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
// Used on the login page when we came back because of a 401.
// Stores the new token and returns true on success, false otherwise.
export async function refreshAccessToken() {
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
