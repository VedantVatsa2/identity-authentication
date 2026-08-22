import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { AuthProvider } from './AuthProvider'
import { useAuth } from './useAuth'
import { authApi } from '../api/auth/authApi'

function TestComponent() {
  const { status, user } = useAuth()
  return (
    <div>
      <span data-testid="status">{status}</span>
      <span data-testid="userId">{user?.userId || 'none'}</span>
    </div>
  )
}

describe('AuthProvider', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('initializes to AUTHENTICATED if refresh returns access token', async () => {
    vi.spyOn(authApi, 'refresh').mockResolvedValue({
      userId: 'test-user-id',
      sessionId: 'test-session-id',
      accessToken: 'test-token',
    })

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    )

    expect(screen.getByTestId('status').textContent).toBe('INITIALIZING')

    await waitFor(() => {
      expect(screen.getByTestId('status').textContent).toBe('AUTHENTICATED')
      expect(screen.getByTestId('userId').textContent).toBe('test-user-id')
    })
  })

  it('initializes to UNAUTHENTICATED if refresh fails', async () => {
    vi.spyOn(authApi, 'refresh').mockRejectedValue(new Error('No refresh cookie'))

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    )

    await waitFor(() => {
      expect(screen.getByTestId('status').textContent).toBe('UNAUTHENTICATED')
      expect(screen.getByTestId('userId').textContent).toBe('none')
    })
  })
})
