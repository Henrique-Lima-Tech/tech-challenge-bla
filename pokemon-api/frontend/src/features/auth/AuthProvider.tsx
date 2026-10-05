import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { notify } from '../../shared/notify'
import { setUnauthorizedHandler } from '../../shared/api/httpClient'
import { tokenStorage } from '../../shared/api/tokenStorage'
import { authApi } from './api'
import { AuthContext, type AuthContextValue, type AuthStatus } from './AuthContext'
import type { LoginRequest } from './types'

/**
 * Holds whether the user is logged in and their name. The session is the token and the name saved
 * at login (D-32): the contract has no endpoint that returns the user.
 */
export function AuthProvider({ children }: Readonly<{ children: ReactNode }>) {
  const queryClient = useQueryClient()
  // The token is not checked here: an expired one is caught by the first 401.
  const [status, setStatus] = useState<AuthStatus>(() =>
    tokenStorage.get() ? 'authenticated' : 'anonymous',
  )
  const [name, setName] = useState<string | null>(() => tokenStorage.getName())

  const logout = useCallback(() => {
    tokenStorage.clear()
    setStatus('anonymous')
    setName(null)
    // Drops the cached data of the previous session.
    queryClient.clear()
  }, [queryClient])

  useEffect(() => {
    setUnauthorizedHandler(() => {
      logout()
      // A fixed id shows the toast once, even when several requests get a 401 together.
      notify.error('Your session has expired. Please sign in again.', { id: 'session-expired' })
    })
    return () => setUnauthorizedHandler(null)
  }, [logout])

  const login = useCallback(async (credentials: LoginRequest) => {
    const { accessToken, name } = await authApi.login(credentials)
    tokenStorage.set(accessToken)
    tokenStorage.setName(name)
    setName(name)
    setStatus('authenticated')
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ status, name, login, logout }),
    [status, name, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
