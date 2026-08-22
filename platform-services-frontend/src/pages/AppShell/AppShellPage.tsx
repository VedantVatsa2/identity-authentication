import { useState } from 'react'
import { useAuth } from '../../auth/useAuth'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { LogOut, ShieldCheck, UserCheck, Key, CheckCircle2 } from 'lucide-react'

export function AppShellPage() {
  const { user, status, logout } = useAuth()
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  const handleLogout = async () => {
    setIsLoggingOut(true)
    try {
      await logout()
    } finally {
      setIsLoggingOut(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      {/* Header Bar */}
      <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur-md sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-indigo-600/10 border border-indigo-500/20 text-indigo-400">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <span className="font-bold text-lg text-white tracking-tight">
              Startup Platform
            </span>
            <span className="text-xs px-2.5 py-0.5 rounded-full font-medium bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
              Authenticated Shell
            </span>
          </div>

          <Button
            variant="outline"
            size="sm"
            isLoading={isLoggingOut}
            onClick={handleLogout}
            className="hover:border-rose-500/50 hover:text-rose-400"
          >
            <LogOut className="w-4 h-4 mr-1.5" />
            Sign Out
          </Button>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-4xl w-full mx-auto p-4 sm:p-6 lg:p-8 space-y-6">
        <div className="space-y-2">
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
            Authentication Shell Verified
          </h1>
          <p className="text-sm text-slate-400">
            You are securely authenticated. Access token is held in memory; refresh token is secured by HttpOnly browser cookie.
          </p>
        </div>

        <div className="grid gap-6 md:grid-cols-2">
          {/* User Session Information */}
          <Card className="space-y-4">
            <div className="flex items-center gap-2 text-indigo-400 font-semibold text-sm border-b border-slate-800 pb-3">
              <UserCheck className="w-4 h-4" />
              Active Session Metadata
            </div>

            <div className="space-y-3 text-sm">
              <div>
                <span className="text-xs uppercase font-medium text-slate-400 block mb-0.5">User ID</span>
                <code className="text-xs sm:text-sm bg-slate-950 px-2.5 py-1.5 rounded-lg border border-slate-800 font-mono text-slate-200 block truncate">
                  {user?.userId || 'N/A'}
                </code>
              </div>

              <div>
                <span className="text-xs uppercase font-medium text-slate-400 block mb-0.5">Session ID</span>
                <code className="text-xs sm:text-sm bg-slate-950 px-2.5 py-1.5 rounded-lg border border-slate-800 font-mono text-slate-200 block truncate">
                  {user?.sessionId || 'N/A'}
                </code>
              </div>

              <div>
                <span className="text-xs uppercase font-medium text-slate-400 block mb-0.5">Auth State Status</span>
                <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-semibold bg-emerald-500/10 border border-emerald-500/30 text-emerald-400">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  {status}
                </div>
              </div>
            </div>
          </Card>

          {/* Security Architecture Summary */}
          <Card className="space-y-4">
            <div className="flex items-center gap-2 text-indigo-400 font-semibold text-sm border-b border-slate-800 pb-3">
              <Key className="w-4 h-4" />
              Security Architecture
            </div>

            <ul className="space-y-2.5 text-xs text-slate-300">
              <li className="flex items-start gap-2">
                <span className="text-indigo-400 font-bold">•</span>
                <span><strong>Access Token:</strong> Stored strictly in memory, attached as Bearer header on API calls.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-indigo-400 font-bold">•</span>
                <span><strong>Refresh Token:</strong> Managed exclusively via HttpOnly, Secure, SameSite=Strict cookie.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-indigo-400 font-bold">•</span>
                <span><strong>Auto-Refresh:</strong> Intercepts 401, issues refresh, and retries request safely with mutex lock.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-indigo-400 font-bold">•</span>
                <span><strong>Logout:</strong> Revokes session server-side and clears memory token.</span>
              </li>
            </ul>
          </Card>
        </div>
      </main>
    </div>
  )
}
