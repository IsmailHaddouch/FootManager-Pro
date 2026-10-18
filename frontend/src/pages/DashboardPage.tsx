import { useAuth } from '../auth/AuthContext'

export default function DashboardPage() {
  const { user } = useAuth()
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-navy">Bonjour, {user?.fullName}</h1>
      <div className="rounded-xl border border-dashed border-slate-300 bg-white p-6 text-slate-600">
        Le tableau de bord de ce rôle sera construit dans les phases suivantes du plan de développement
        (terrains, calendrier, réservations…).
      </div>
    </div>
  )
}
