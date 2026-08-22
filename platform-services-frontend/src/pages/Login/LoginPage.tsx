import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { ApiError } from '../../api/auth/authTypes'
import { Button } from '../../components/ui/Button'
import { Input } from '../../components/ui/Input'
import { Alert } from '../../components/ui/Alert'
import { Card } from '../../components/ui/Card'
import { Lock, LogIn } from 'lucide-react'

const loginSchema = z.object({
  email: z
    .string()
    .min(1, 'Email is required')
    .email('Please enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})

type LoginFormData = z.infer<typeof loginSchema>

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [serverError, setServerError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  })

  const onSubmit = async (data: LoginFormData) => {
    setServerError(null)
    try {
      await login({
        email: data.email,
        password: data.password,
      })
      navigate('/', { replace: true })
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.code === 'AUTHENTICATION_FAILED') {
          setServerError('Invalid email address or password.')
        } else {
          setServerError(err.detail || err.title || 'Login failed. Please try again.')
        }
      } else {
        setServerError('An unexpected network error occurred. Please try again.')
      }
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-slate-950">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center space-y-2">
          <div className="inline-flex p-3 rounded-2xl bg-indigo-600/10 border border-indigo-500/20 text-indigo-400 mb-1">
            <Lock className="w-8 h-8" />
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
            Welcome Back
          </h1>
          <p className="text-sm text-slate-400">
            Sign in to access your Startup Platform account
          </p>
        </div>

        <Card>
          {serverError && (
            <Alert type="error" title="Authentication Failed" className="mb-6">
              {serverError}
            </Alert>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>
            <Input
              label="Email Address"
              type="email"
              placeholder="name@example.com"
              autoComplete="email"
              error={errors.email?.message}
              {...register('email')}
            />

            <Input
              label="Password"
              type="password"
              placeholder="••••••••••••"
              autoComplete="current-password"
              error={errors.password?.message}
              {...register('password')}
            />

            <Button
              type="submit"
              fullWidth
              size="lg"
              isLoading={isSubmitting}
              className="mt-2"
            >
              <LogIn className="w-4 h-4 mr-2" />
              Sign In
            </Button>
          </form>

          <div className="mt-6 space-y-3 text-center text-xs text-slate-400 pt-4 border-t border-slate-800">
            <div>
              Need an account?{' '}
              <Link
                to="/register"
                className="font-semibold text-indigo-400 hover:text-indigo-300 underline underline-offset-4"
              >
                Create Account
              </Link>
            </div>
            <div>
              Have a verification code?{' '}
              <Link
                to="/verify-email"
                className="font-semibold text-indigo-400 hover:text-indigo-300 underline underline-offset-4"
              >
                Verify Email
              </Link>
            </div>
          </div>
        </Card>
      </div>
    </div>
  )
}
