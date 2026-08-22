import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { VerifyEmailPage } from './VerifyEmailPage'
import { AuthContext } from '../../auth/AuthContext'
import { ApiError } from '../../api/auth/authTypes'

describe('VerifyEmailPage', () => {
  const mockVerifyEmail = vi.fn()

  const defaultContext = {
    status: 'UNAUTHENTICATED' as const,
    user: null,
    login: vi.fn(),
    register: vi.fn(),
    verifyEmail: mockVerifyEmail,
    logout: vi.fn(),
  }

  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('does not display Challenge ID as a user-entered field', () => {
    render(
      <AuthContext.Provider value={defaultContext}>
        <MemoryRouter>
          <VerifyEmailPage />
        </MemoryRouter>
      </AuthContext.Provider>
    )

    expect(screen.queryByLabelText(/challenge id/i)).not.toBeInTheDocument()
    expect(screen.getByLabelText(/6-digit otp code/i)).toBeInTheDocument()
  })

  it('handles missing challengeId in URL parameter', async () => {
    render(
      <AuthContext.Provider value={defaultContext}>
        <MemoryRouter initialEntries={['/verify-email']}>
          <VerifyEmailPage />
        </MemoryRouter>
      </AuthContext.Provider>
    )

    expect(screen.getByText(/missing challenge id/i)).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/6-digit otp code/i), {
      target: { value: '123456' },
    })

    fireEvent.click(screen.getByRole('button', { name: /verify email/i }))

    await waitFor(() => {
      expect(screen.getByText(/no challenge ID was provided in the URL query parameter/i)).toBeInTheDocument()
    })
    expect(mockVerifyEmail).not.toHaveBeenCalled()
  })

  it('handles malformed challengeId in URL parameter', async () => {
    render(
      <AuthContext.Provider value={defaultContext}>
        <MemoryRouter initialEntries={['/verify-email?challengeId=not-a-valid-uuid']}>
          <VerifyEmailPage />
        </MemoryRouter>
      </AuthContext.Provider>
    )

    expect(screen.getByText(/invalid challenge id link/i)).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/6-digit otp code/i), {
      target: { value: '123456' },
    })

    fireEvent.click(screen.getByRole('button', { name: /verify email/i }))

    await waitFor(() => {
      expect(screen.getByText(/malformed or invalid/i)).toBeInTheDocument()
    })
    expect(mockVerifyEmail).not.toHaveBeenCalled()
  })

  it('validates invalid OTP format (non-numeric or not 6 digits)', async () => {
    const validChallengeId = '8788d19d-48d0-415d-80fd-241c92003a27'

    render(
      <AuthContext.Provider value={defaultContext}>
        <MemoryRouter initialEntries={[`/verify-email?challengeId=${validChallengeId}`]}>
          <VerifyEmailPage />
        </MemoryRouter>
      </AuthContext.Provider>
    )

    fireEvent.change(screen.getByLabelText(/6-digit otp code/i), {
      target: { value: '123' },
    })

    fireEvent.click(screen.getByRole('button', { name: /verify email/i }))

    await waitFor(() => {
      expect(screen.getByText(/verification code must be exactly 6 digits/i)).toBeInTheDocument()
    })
    expect(mockVerifyEmail).not.toHaveBeenCalled()
  })

  it('submits valid challengeId + 6-digit OTP and displays success state', async () => {
    mockVerifyEmail.mockResolvedValue({
      status: 'ACTIVE',
      message: 'Email verification successful.',
    })

    const validChallengeId = '8788d19d-48d0-415d-80fd-241c92003a27'

    render(
      <AuthContext.Provider value={defaultContext}>
        <MemoryRouter initialEntries={[`/verify-email?challengeId=${validChallengeId}`]}>
          <VerifyEmailPage />
        </MemoryRouter>
      </AuthContext.Provider>
    )

    fireEvent.change(screen.getByLabelText(/6-digit otp code/i), {
      target: { value: '654321' },
    })

    fireEvent.click(screen.getByRole('button', { name: /verify email/i }))

    await waitFor(() => {
      expect(mockVerifyEmail).toHaveBeenCalledWith({
        challengeId: validChallengeId,
        otp: '654321',
      })
      expect(screen.getByText(/email verified successfully!/i)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /proceed to login/i })).toBeInTheDocument()
    })
  })

  it('handles backend verification failure gracefully', async () => {
    mockVerifyEmail.mockRejectedValue(
      new ApiError({
        status: 400,
        code: 'VERIFICATION_FAILED',
        title: 'Verification Failed',
        detail: 'Invalid or expired OTP',
        type: 'https://platform.example.com/errors/verification-failed',
        timestamp: '2026-08-22T12:00:00Z',
      })
    )

    const validChallengeId = '8788d19d-48d0-415d-80fd-241c92003a27'

    render(
      <AuthContext.Provider value={defaultContext}>
        <MemoryRouter initialEntries={[`/verify-email?challengeId=${validChallengeId}`]}>
          <VerifyEmailPage />
        </MemoryRouter>
      </AuthContext.Provider>
    )

    fireEvent.change(screen.getByLabelText(/6-digit otp code/i), {
      target: { value: '999999' },
    })

    fireEvent.click(screen.getByRole('button', { name: /verify email/i }))

    await waitFor(() => {
      expect(mockVerifyEmail).toHaveBeenCalledWith({
        challengeId: validChallengeId,
        otp: '999999',
      })
      expect(screen.getByText(/invalid or expired verification code/i)).toBeInTheDocument()
    })
  })
})
