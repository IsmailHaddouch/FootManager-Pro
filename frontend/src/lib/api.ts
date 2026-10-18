const TOKEN_KEY = 'fm_token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  const token = tokenStore.get()
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const res = await fetch(`/api${path}`, { ...options, headers })
  if (!res.ok) {
    let message = 'Une erreur est survenue'
    try {
      const body = await res.json()
      message = body.message ?? message
    } catch {
      /* corps vide */
    }
    throw new ApiError(res.status, res.status === 401 ? 'Identifiants invalides ou session expirée' : message)
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T)
}
