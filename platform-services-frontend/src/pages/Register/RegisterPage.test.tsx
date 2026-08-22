import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { RegisterPage } from './RegisterPage'
import { AuthContext } from '../../auth/AuthContext'

describe('RegisterPage', () => {
  const mockRegister = vi.fn()

  const defaultContext = {
    status: 'UNAUTHENTICATED' as const,
    user: null,
    login: vi.fn(),
    register: mockRegister,
    verifyEmail: vi.fn(),
    logout: vi.fn(),
  }

  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('shows error when passwords do not match', async () => {
    render(
      <AuthContext.Provider value={defaultContext}>
        <MemoryRouter>
          <RegisterPage />
        </MemoryRouter>
      </AuthContext.Provider>
    )

    fireEvent.change(screen.getByLabelText(/email address/i), {
      target: { value: 'user@example.com' },
    })
    fireEvent.change(screen.getByLabelText(/^password$/i), {
      target: { value: 'Password123!' },
    })
    fireEvent.change(screen.getByLabelText(/confirm password/i), {
      target: { value: 'DifferentPassword123!' },
    })

    fireEvent.click(screen.getByRole('button', { name: /register account/i }))

    await waitFor(() => {
      expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument()
    })
    expect(mockRegister).not.toHaveBeenCalled()
  })

  it('submits registration successfully when inputs are valid', async () => {
    mockRegister.mockResolvedValue({
      userId: '8788d19d-48d0-415d-80fd-241c92003a27',
      status: 'PENDING_VERIFICATION',
    })

    render(
      <AuthContext.Provider value={defaultContext}>
        <MemoryRouter>
          <RegisterPage />
        </MemoryRouter>
      </AuthContext.Provider>
    )

    fireEvent.change(screen.getByLabelText(/email address/i), {
      target: { value: 'user@example.com' },
    })
    fireEvent.change(screen.getByLabelText(/^password$/i), {
      target: { value: 'CorrectHorseBatteryStaple!123' },
    })
    fireEvent.change(screen.getByLabelText(/confirm password/i), {
      target: { value: 'CorrectHorseBatteryStaple!123' },
    })

    fireEvent.click(screen.getByRole('button', { name: /register account/i }))

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith({
        email: 'user@example.com',
        password: 'CorrectHorseBatteryStaple!123',
      })
    })
  })
})
