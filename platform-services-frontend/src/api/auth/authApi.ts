import { apiClient } from '../client/apiClient'
import { setAccessToken } from '../client/tokenManager'
import {
  RegisterRequest,
  RegisterResponse,
  VerifyEmailRequest,
  VerifyEmailResponse,
  LoginRequest,
  LoginResponse,
  RefreshResponse,
} from './authTypes'

export const authApi = {
  async register(payload: RegisterRequest): Promise<RegisterResponse> {
    return apiClient<RegisterResponse>('/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  async verifyEmail(payload: VerifyEmailRequest): Promise<VerifyEmailResponse> {
    return apiClient<VerifyEmailResponse>('/v1/auth/verify', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  async login(payload: LoginRequest): Promise<LoginResponse> {
    const data = await apiClient<LoginResponse>('/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    if (data?.accessToken) {
      setAccessToken(data.accessToken)
    }
    return data
  },

  async refresh(): Promise<RefreshResponse> {
    const data = await apiClient<RefreshResponse>('/v1/auth/refresh', {
      method: 'POST',
    })
    if (data?.accessToken) {
      setAccessToken(data.accessToken)
    }
    return data
  },

  async logout(): Promise<void> {
    try {
      await apiClient<void>('/v1/auth/logout', {
        method: 'POST',
      })
    } finally {
      setAccessToken(null)
    }
  },
}
