import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { apiClient } from './apiClient'
import { getAccessToken, setAccessToken } from './tokenManager'
import { ApiError } from '../auth/authTypes'

describe('apiClient', () => {
  beforeEach(() => {
    setAccessToken(null)
    vi.restoreAllMocks()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('attaches Authorization header when access token is present', async () => {
    setAccessToken('mock-access-token')

    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ success: true }),
    })
    vi.stubGlobal('fetch', mockFetch)

    const res = await apiClient<{ success: boolean }>('/v1/test')

    expect(res).toEqual({ success: true })
    expect(mockFetch).toHaveBeenCalledWith(
      '/v1/test',
      expect.objectContaining({
        credentials: 'include',
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
          Authorization: 'Bearer mock-access-token',
        }),
      })
    )
  })

  it('parses RFC 7807 problem details into ApiError on HTTP error', async () => {
    const problemDetail = {
      status: 401,
      title: 'Refresh Token Invalid',
      detail: 'Invalid refresh token.',
      type: 'urn:startup:auth:error:invalid-refresh-token',
      code: 'INVALID_REFRESH_TOKEN',
      timestamp: '2026-08-22T00:00:00Z',
    }

    const mockFetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => problemDetail,
    })
    vi.stubGlobal('fetch', mockFetch)

    try {
      await apiClient('/v1/auth/refresh')
      expect.fail('Should have thrown ApiError')
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError)
      const apiErr = err as ApiError
      expect(apiErr.status).toBe(401)
      expect(apiErr.code).toBe('INVALID_REFRESH_TOKEN')
      expect(apiErr.detail).toBe('Invalid refresh token.')
    }
  })

  it('handles 401 by attempting token refresh once and retrying original request', async () => {
    setAccessToken('expired-token')

    const mockFetch = vi.fn().mockImplementation(async (url: string) => {
      if (url.includes('/v1/auth/refresh')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            userId: 'user-123',
            sessionId: 'session-456',
            accessToken: 'new-fresh-access-token',
          }),
        }
      }
      if (url.includes('/v1/protected-endpoint')) {
        const authHeader = (mockFetch.mock.calls[mockFetch.mock.calls.length - 1][1]?.headers as Record<string, string>)?.Authorization
        if (authHeader === 'Bearer new-fresh-access-token') {
          return {
            ok: true,
            status: 200,
            json: async () => ({ data: 'protected-data' }),
          }
        }
        return {
          ok: false,
          status: 401,
          json: async () => ({
            status: 401,
            title: 'Unauthorized',
            detail: 'Expired token',
            code: 'TOKEN_EXPIRED',
          }),
        }
      }
      return { ok: false, status: 404 }
    })

    vi.stubGlobal('fetch', mockFetch)

    const result = await apiClient<{ data: string }>('/v1/protected-endpoint')

    expect(result).toEqual({ data: 'protected-data' })
    expect(getAccessToken()).toBe('new-fresh-access-token')
  })
})
