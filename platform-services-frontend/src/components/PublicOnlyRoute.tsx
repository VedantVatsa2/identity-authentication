import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { Spinner } from './ui/Spinner'

export function PublicOnlyRoute() {
  const { status } = useAuth()

  if (status === 'INITIALIZING') {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-slate-950 text-slate-300">
        <Spinner size="lg" className="text-indigo-500 mb-4" />
        <p className="text-sm font-medium animate-pulse text-slate-400">
          Initializing authentication...
        </p>
      </div>
    )
  }

  if (status === 'AUTHENTICATED') {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
