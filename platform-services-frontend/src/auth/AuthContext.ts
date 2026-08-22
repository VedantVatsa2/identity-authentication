import { createContext } from 'react'
import {
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
  VerifyEmailRequest,
  VerifyEmailResponse,
} from '../api/auth/authTypes'

export type AuthStatus = 'INITIALIZING' | 'UNAUTHENTICATED' | 'AUTHENTICATED'

export interface UserSession {
  userId: string
  sessionId: string
}

export interface AuthContextValue {
  status: AuthStatus
  user: UserSession | null
  login: (credentials: LoginRequest) => Promise<void>
  register: (data: RegisterRequest) => Promise<RegisterResponse>
  verifyEmail: (data: VerifyEmailRequest) => Promise<VerifyEmailResponse>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
