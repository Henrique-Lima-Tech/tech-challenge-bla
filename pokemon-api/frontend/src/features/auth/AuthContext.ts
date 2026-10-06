import { createContext } from 'react'
import type { LoginRequest } from './types'

export type AuthStatus = 'authenticated' | 'anonymous'

export type AuthContextValue = {
  status: AuthStatus
  name: string | null
  login: (credentials: LoginRequest) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
