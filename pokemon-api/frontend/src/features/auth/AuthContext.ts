import { createContext } from 'react'
import type { LoginRequest } from './types'

export type AuthStatus = 'authenticated' | 'anonymous'

export type AuthContextValue = {
  status: AuthStatus
  /** null for a session saved before the login returned the name (D-32). */
  name: string | null
  login: (credentials: LoginRequest) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
