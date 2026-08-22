let inMemoryAccessToken: string | null = null
let authFailureListeners: Array<() => void> = []

export const getAccessToken = (): string | null => inMemoryAccessToken

export const setAccessToken = (token: string | null): void => {
  inMemoryAccessToken = token
}

export const notifyAuthFailure = (): void => {
  inMemoryAccessToken = null
  authFailureListeners.forEach(listener => {
    try {
      listener()
    } catch {
      // Ignore listener errors
    }
  })
}

export const onAuthFailure = (listener: () => void): (() => void) => {
  authFailureListeners.push(listener)
  return () => {
    authFailureListeners = authFailureListeners.filter(l => l !== listener)
  }
}
