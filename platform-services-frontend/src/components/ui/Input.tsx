import { InputHTMLAttributes, forwardRef, useId } from 'react'

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  helperText?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, helperText, className = '', id: customId, ...props }, ref) => {
    const generatedId = useId()
    const inputId = customId || generatedId
    const errorId = `${inputId}-error`
    const helperId = `${inputId}-helper`

    const describedBy = [
      error ? errorId : null,
      helperText ? helperId : null,
    ]
      .filter(Boolean)
      .join(' ')

    return (
      <div className="w-full space-y-1.5">
        <label
          htmlFor={inputId}
          className="block text-xs font-semibold uppercase tracking-wider text-slate-300"
        >
          {label}
        </label>

        <div className="relative">
          <input
            ref={ref}
            id={inputId}
            aria-invalid={!!error}
            aria-describedby={describedBy || undefined}
            className={`w-full rounded-lg bg-slate-900/90 border px-3.5 py-2.5 text-sm text-slate-100 placeholder-slate-500 transition-colors duration-150 focus:outline-none focus:ring-2 ${
              error
                ? 'border-rose-500/80 focus:border-rose-500 focus:ring-rose-500/30'
                : 'border-slate-800 focus:border-indigo-500 focus:ring-indigo-500/30 hover:border-slate-700'
            } ${className}`}
            {...props}
          />
        </div>

        {helperText && !error && (
          <p id={helperId} className="text-xs text-slate-400">
            {helperText}
          </p>
        )}

        {error && (
          <p id={errorId} role="alert" className="text-xs font-medium text-rose-400">
            {error}
          </p>
        )}
      </div>
    )
  }
)

Input.displayName = 'Input'
