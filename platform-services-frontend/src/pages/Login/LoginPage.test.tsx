import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { LoginPage } from './LoginPage'
import { AuthContext } from '../../auth/AuthContext'
import { ApiError } from '../../api/auth/authTypes'

describe('LoginPage', () => {
  const mockLogin = vi.fn()
  const mockRegister = vi.fn()
  const mockVerifyEmail = vi.fn()
  const mockLogout = vi.fn()

  const defaultContext = {
    status: 'UNAUTHENTICATED' as const,
    user: null,
    login: mockLogin,
    register: mockRegister,
    verifyEmail: mockVerifyEmail,
    logout: mockLogout,
  }

  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('renders login form with email and password fields', () => {
    render(
      <AuthContext.Provider value={defaultContext}>
        <MemoryRouter>
          <LoginPage />
        </MemoryRouter>
      </AuthContext.Provider>
    )

    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
  })

  it('shows validation error when submitting empty form', async () => {
    render(
      <AuthContext.Provider value={defaultContext}>
        <MemoryRouter>
          <LoginPage />
        </MemoryRouter>
      </AuthContext.Provider>
    )

    fireEvent.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => {
      expect(screen.getByText(/email is required/i)).toBeInTheDocument()
      expect(screen.getByText(/password is required/i)).toBeInTheDocument()
    })
    expect(mockLogin).not.toHaveBeenCalled()
  })

  it('handles backend AUTHENTICATION_FAILED error gracefully', async () => {
    mockLogin.mockRejectedValue(
      new ApiError({
        status: 401,
        title: 'Authentication Failed',
        detail: 'Invalid email or password.',
        type: 'urn:startup:auth:error:authentication-failed',
        code: 'AUTHENTICATION_FAILED',
        timestamp: new Date().toISOString(),
      })
    )

    render(
      <AuthContext.Provider value={defaultContext}>
        <MemoryRouter>
          <LoginPage />
        </MemoryRouter>
      </AuthContext.Provider>
    )

    fireEvent.change(screen.getByLabelText(/email address/i), {
      target: { value: 'wrong@example.com' },
    })
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'wrongpassword' },
    })

    fireEvent.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => {
      expect(screen.getByText(/invalid email address or password/i)).toBeInTheDocument()
    })
  })
})
