import { ReactNode } from 'react'
import { AlertCircle, CheckCircle2, Info, XCircle } from 'lucide-react'

export interface AlertProps {
  type?: 'error' | 'success' | 'warning' | 'info'
  title?: string
  children: ReactNode
  className?: string
}

export function Alert({
  type = 'error',
  title,
  children,
  className = '',
}: AlertProps) {
  const styles = {
    error: {
      container: 'bg-rose-950/40 border-rose-800/60 text-rose-200',
      icon: <XCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />,
      titleColor: 'text-rose-300',
    },
    success: {
      container: 'bg-emerald-950/40 border-emerald-800/60 text-emerald-200',
      icon: <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />,
      titleColor: 'text-emerald-300',
    },
    warning: {
      container: 'bg-amber-950/40 border-amber-800/60 text-amber-200',
      icon: <AlertCircle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />,
      titleColor: 'text-amber-300',
    },
    info: {
      container: 'bg-indigo-950/40 border-indigo-800/60 text-indigo-200',
      icon: <Info className="w-5 h-5 text-indigo-400 shrink-0 mt-0.5" />,
      titleColor: 'text-indigo-300',
    },
  }

  const activeStyle = styles[type]

  return (
    <div
      role="alert"
      className={`flex items-start gap-3 p-4 rounded-xl border text-sm backdrop-blur-sm shadow-sm ${activeStyle.container} ${className}`}
    >
      {activeStyle.icon}
      <div className="space-y-1">
        {title && <h5 className={`font-semibold ${activeStyle.titleColor}`}>{title}</h5>}
        <div className="leading-relaxed">{children}</div>
      </div>
    </div>
  )
}
