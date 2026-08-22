import { useState, useEffect, useCallback, ReactNode } from 'react'
import { AuthContext, AuthStatus, UserSession } from './AuthContext'
import { authApi } from '../api/auth/authApi'
import { onAuthFailure, setAccessToken } from '../api/client/tokenManager'
import {
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
  VerifyEmailRequest,
  VerifyEmailResponse,
} from '../api/auth/authTypes'

interface AuthProviderProps {
  children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [status, setStatus] = useState<AuthStatus>('INITIALIZING')
  const [user, setUser] = useState<UserSession | null>(null)

  // Initialize auth state on mount by attempting refresh
  useEffect(() => {
    let isMounted = true

    async function initAuth() {
      try {
        const res = await authApi.refresh()
        if (isMounted && res.accessToken) {
          setUser({ userId: res.userId, sessionId: res.sessionId })
          setStatus('AUTHENTICATED')
        } else if (isMounted) {
          setUser(null)
          setStatus('UNAUTHENTICATED')
        }
      } catch {
        if (isMounted) {
          setAccessToken(null)
          setUser(null)
          setStatus('UNAUTHENTICATED')
        }
      }
    }

    initAuth()

    // Listen for unexpected authentication failures (e.g. invalid refresh)
    const unsubscribe = onAuthFailure(() => {
      if (isMounted) {
        setUser(null)
        setStatus('UNAUTHENTICATED')
      }
    })

    return () => {
      isMounted = false
      unsubscribe()
    }
  }, [])

  const login = useCallback(async (credentials: LoginRequest) => {
    const res = await authApi.login(credentials)
    setUser({ userId: res.userId, sessionId: res.sessionId })
    setStatus('AUTHENTICATED')
  }, [])

  const register = useCallback(async (data: RegisterRequest): Promise<RegisterResponse> => {
    return await authApi.register(data)
  }, [])

  const verifyEmail = useCallback(async (data: VerifyEmailRequest): Promise<VerifyEmailResponse> => {
    return await authApi.verifyEmail(data)
  }, [])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      setUser(null)
      setStatus('UNAUTHENTICATED')
    }
  }, [])

  return (
    <AuthContext.Provider
      value={{
        status,
        user,
        login,
        register,
        verifyEmail,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}
