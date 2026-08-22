/**
 * RFC 7807 Problem Detail Error structure returned by the Spring Boot backend
 */
export interface ProblemDetail {
  status: number
  title: string
  detail: string
  type: string
  code: string
  timestamp: string
  instance?: string
}

/**
 * Custom Error class for typed frontend API error handling
 */
export class ApiError extends Error {
  public readonly status: number
  public readonly code: string
  public readonly title: string
  public readonly detail: string
  public readonly type: string
  public readonly timestamp: string
  public readonly instance?: string

  constructor(problem: ProblemDetail) {
    super(problem.detail || problem.title || 'An unexpected API error occurred')
    this.name = 'ApiError'
    this.status = problem.status
    this.code = problem.code
    this.title = problem.title
    this.detail = problem.detail
    this.type = problem.type
    this.timestamp = problem.timestamp
    this.instance = problem.instance
  }
}

// User status enum matching backend AuthUser.Status
export type UserStatus = 'PENDING_VERIFICATION' | 'ACTIVE' | 'SUSPENDED' | 'DEACTIVATED'

// Request & Response DTOs matching backend auth-service contracts

export interface RegisterRequest {
  email: string
  password: string
}

export interface RegisterResponse {
  userId: string
  status: UserStatus
}

export interface VerifyEmailRequest {
  challengeId: string
  otp: string
}

export interface VerifyEmailResponse {
  status: UserStatus
  message: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  userId: string
  sessionId: string
  accessToken: string
}

export interface RefreshResponse {
  userId: string
  sessionId: string
  accessToken: string
}
