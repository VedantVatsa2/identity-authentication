import { ApiError, ProblemDetail } from '../auth/authTypes'
import { getAccessToken, setAccessToken, notifyAuthFailure } from './tokenManager'

const rawBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
const API_BASE_URL = rawBaseUrl.replace(/\/$/, '')

interface RequestOptions extends RequestInit {
  isRetry?: boolean
  skipAuthHeader?: boolean
}

let refreshPromise: Promise<string> | null = null

function buildUrl(endpoint: string): string {
  if (endpoint.startsWith('http')) {
    return endpoint
  }
  const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`
  return API_BASE_URL ? `${API_BASE_URL}${cleanEndpoint}` : cleanEndpoint
}

async function performRefresh(): Promise<string> {
  const response = await fetch(buildUrl('/v1/auth/refresh'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  })

  if (!response.ok) {
    const error = await parseApiError(response)
    throw error
  }

  const data = await response.json()
  if (!data.accessToken) {
    throw new ApiError({
      status: 500,
      title: 'Invalid Refresh Response',
      detail: 'Backend refresh response did not contain access token.',
      type: 'urn:startup:auth:error:invalid-refresh-response',
      code: 'INVALID_REFRESH_RESPONSE',
      timestamp: new Date().toISOString(),
    })
  }

  setAccessToken(data.accessToken)
  return data.accessToken
}

async function parseApiError(response: Response): Promise<ApiError> {
  try {
    const data = await response.json()
    if (data && typeof data === 'object' && 'code' in data && 'title' in data) {
      return new ApiError(data as ProblemDetail)
    }
    return new ApiError({
      status: response.status,
      title: response.statusText || 'API Error',
      detail: (data && data.message) || (data && data.detail) || `HTTP error ${response.status}`,
      type: `urn:startup:auth:error:http-${response.status}`,
      code: `HTTP_${response.status}`,
      timestamp: new Date().toISOString(),
    })
  } catch {
    return new ApiError({
      status: response.status,
      title: response.statusText || 'API Error',
      detail: `HTTP error ${response.status}`,
      type: `urn:startup:auth:error:http-${response.status}`,
      code: `HTTP_${response.status}`,
      timestamp: new Date().toISOString(),
    })
  }
}

export async function apiClient<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const { isRetry = false, skipAuthHeader = false, headers: customHeaders, ...restOptions } = options

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(customHeaders as Record<string, string>),
  }

  const token = getAccessToken()
  if (token && !skipAuthHeader) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const url = buildUrl(endpoint)

  const requestOptions: RequestInit = {
    ...restOptions,
    headers,
    credentials: 'include',
  }

  let response: Response
  try {
    response = await fetch(url, requestOptions)
  } catch (err) {
    throw new ApiError({
      status: 0,
      title: 'Network Error',
      detail: err instanceof Error ? err.message : 'Failed to connect to backend server.',
      type: 'urn:startup:auth:error:network-error',
      code: 'NETWORK_ERROR',
      timestamp: new Date().toISOString(),
    })
  }

  // Handle 401 Unauthorized for automatic token refresh (only once per request)
  const isAuthEndpoint = endpoint.includes('/v1/auth/login') || endpoint.includes('/v1/auth/refresh')
  if (response.status === 401 && !isAuthEndpoint && !isRetry) {
    try {
      if (!refreshPromise) {
        refreshPromise = performRefresh().finally(() => {
          refreshPromise = null
        })
      }

      await refreshPromise

      // Retry original request once with new token
      return apiClient<T>(endpoint, {
        ...options,
        isRetry: true,
      })
    } catch (refreshErr) {
      notifyAuthFailure()
      throw refreshErr
    }
  }

  if (!response.ok) {
    const apiError = await parseApiError(response)
    if (response.status === 401 && isAuthEndpoint) {
      notifyAuthFailure()
    }
    throw apiError
  }

  // Support 204 No Content
  if (response.status === 204) {
    return undefined as unknown as T
  }

  return response.json()
}
