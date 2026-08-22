import { useState, useMemo } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useSearchParams } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { ApiError } from '../../api/auth/authTypes'
import { Button } from '../../components/ui/Button'
import { Input } from '../../components/ui/Input'
import { Alert } from '../../components/ui/Alert'
import { Card } from '../../components/ui/Card'
import { CheckCircle2, KeyRound, ArrowRight } from 'lucide-react'

const uuidSchema = z.string().uuid()

const verifySchema = z.object({
  otp: z
    .string()
    .min(1, 'Verification code is required')
    .regex(/^\d{6}$/, 'Verification code must be exactly 6 digits'),
})

type VerifyFormData = z.infer<typeof verifySchema>

export function VerifyEmailPage() {
  const { verifyEmail } = useAuth()
  const [searchParams] = useSearchParams()
  const rawChallengeId = searchParams.get('challengeId')

  const challengeIdState = useMemo(() => {
    if (!rawChallengeId || !rawChallengeId.trim()) {
      return { status: 'MISSING' as const, value: null }
    }
    const parseResult = uuidSchema.safeParse(rawChallengeId.trim())
    if (!parseResult.success) {
      return { status: 'MALFORMED' as const, value: rawChallengeId }
    }
    return { status: 'VALID' as const, value: parseResult.data }
  }, [rawChallengeId])

  const [serverError, setServerError] = useState<string | null>(null)
  const [isVerified, setIsVerified] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<VerifyFormData>({
    resolver: zodResolver(verifySchema),
    defaultValues: {
      otp: '',
    },
  })

  const onSubmit = async (data: VerifyFormData) => {
    setServerError(null)

    if (challengeIdState.status === 'MISSING') {
      setServerError(
        'Verification cannot be completed from this page because no challenge ID was provided in the URL query parameter (e.g. /verify?challengeId=<UUID>). The backend API contract requires a challenge ID to verify your email, but does not expose an endpoint to retrieve it.'
      )
      return
    }

    if (challengeIdState.status === 'MALFORMED') {
      setServerError(
        `The Challenge ID provided in the URL parameter ("${challengeIdState.value}") is malformed or invalid. A valid UUID format is required.`
      )
      return
    }

    try {
      await verifyEmail({
        challengeId: challengeIdState.value,
        otp: data.otp,
      })
      setIsVerified(true)
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.code === 'VERIFICATION_FAILED') {
          setServerError('Invalid or expired verification code. Please check your 6-digit OTP code.')
        } else {
          setServerError(err.detail || err.title || 'Email verification failed.')
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
            <KeyRound className="w-8 h-8" />
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
            Verify Email Address
          </h1>
          <p className="text-sm text-slate-400">
            Enter your 6-digit verification code to activate your account
          </p>
        </div>

        <Card>
          {isVerified ? (
            <div className="text-center py-4 space-y-5">
              <div className="inline-flex p-4 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
                <CheckCircle2 className="w-12 h-12" />
              </div>
              <div className="space-y-2">
                <h3 className="text-xl font-bold text-white">Email Verified Successfully!</h3>
                <p className="text-sm text-slate-300">
                  Your account status is now <span className="text-emerald-400 font-semibold">ACTIVE</span>. You can proceed to log in.
                </p>
              </div>
              <Link to="/login" className="block pt-2">
                <Button fullWidth size="lg">
                  Proceed to Login
                  <ArrowRight className="w-4 h-4 ml-2" />
                </Button>
              </Link>
            </div>
          ) : (
            <>
              {challengeIdState.status === 'MISSING' && (
                <Alert type="warning" title="Missing Challenge ID" className="mb-6">
                  Verification cannot be completed from this page because no verification challenge ID was provided in the URL query parameter (e.g. <code>/verify?challengeId=&lt;UUID&gt;</code>). The backend API requires a challenge ID to verify your email, but does not expose an endpoint to retrieve it.
                </Alert>
              )}

              {challengeIdState.status === 'MALFORMED' && (
                <Alert type="error" title="Invalid Challenge ID Link" className="mb-6">
                  The Challenge ID in your verification link is invalid or malformed (&quot;{challengeIdState.value}&quot;). A valid UUID parameter is required.
                </Alert>
              )}

              {serverError && (
                <Alert type="error" title="Verification Failed" className="mb-6">
                  {serverError}
                </Alert>
              )}

              <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>
                <Input
                  label="6-Digit OTP Code"
                  type="text"
                  placeholder="123456"
                  maxLength={6}
                  autoComplete="one-time-code"
                  error={errors.otp?.message}
                  helperText="Enter the 6-digit numeric verification code sent to your email"
                  {...register('otp')}
                />

                <Button
                  type="submit"
                  fullWidth
                  size="lg"
                  isLoading={isSubmitting}
                  className="mt-2"
                >
                  Verify Email
                </Button>
              </form>

              <div className="mt-6 text-center text-xs text-slate-400 pt-4 border-t border-slate-800">
                Already verified?{' '}
                <Link
                  to="/login"
                  className="font-semibold text-indigo-400 hover:text-indigo-300 underline underline-offset-4"
                >
                  Sign In
                </Link>
              </div>
            </>
          )}
        </Card>
      </div>
    </div>
  )
}
