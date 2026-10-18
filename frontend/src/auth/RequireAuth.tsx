import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth, type Role } from './AuthContext'

/**
 * Garde de route côté interface (confort d'affichage uniquement).
 * La vraie sécurité reste appliquée côté serveur sur chaque endpoint.
 */
export default function RequireAuth({ children, roles }: { children: ReactNode; roles?: Role[] }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) return <p className="p-8 text-slate-500">Chargement…</p>
  if (!user) return <Navigate to="/login" state={{ from: location }} replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />
  return <>{children}</>
}
