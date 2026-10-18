import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth, type Role } from '../auth/AuthContext'

interface NavItem {
  to: string
  label: string
  roles: Role[]
}

// Les pages marquées "bientôt" seront ajoutées phase par phase (voir plan de développement).
const NAV: NavItem[] = [
  { to: '/', label: 'Tableau de bord', roles: ['SUPER_ADMIN', 'OWNER', 'STAFF', 'CLUB_MANAGER', 'CUSTOMER'] },
]

const ROLE_LABEL: Record<Role, string> = {
  SUPER_ADMIN: 'SuperAdmin',
  OWNER: 'Propriétaire',
  STAFF: 'Personnel',
  CLUB_MANAGER: 'Gestionnaire de club',
  CUSTOMER: 'Client',
}

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  if (!user) return null

  return (
    <div className="flex min-h-screen flex-col md:flex-row">
      <aside className="bg-navy text-white md:w-64 md:shrink-0">
        <div className="flex items-center gap-2 px-6 py-5 text-xl font-bold">
          <span className="inline-block h-3 w-3 rounded-full bg-pitch" />
          FootManager <span className="text-pitch">Pro</span>
        </div>
        <nav className="flex gap-1 overflow-x-auto px-3 pb-3 md:flex-col md:pb-0">
          {NAV.filter((n) => n.roles.includes(user.role)).map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end
              className={({ isActive }) =>
                `rounded-lg px-3 py-2 text-sm font-medium whitespace-nowrap ${
                  isActive ? 'bg-pitch text-navy' : 'text-slate-200 hover:bg-navy-700'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="flex flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-3">
          <div>
            <p className="text-sm font-semibold">{user.fullName}</p>
            <p className="text-xs text-slate-500">{ROLE_LABEL[user.role]}</p>
          </div>
          <button
            onClick={() => {
              logout()
              navigate('/login')
            }}
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm hover:bg-slate-100"
          >
            Déconnexion
          </button>
        </header>
        <main className="flex-1 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
