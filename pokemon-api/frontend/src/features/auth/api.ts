import { http } from '../../shared/api/httpClient'
import type { LoginRequest, RegisterRequest, TokenResponse, User } from './types'

export const authApi = {
  login: (credentials: LoginRequest) => http.post<TokenResponse>('/api/v1/auth/login', credentials),

  register: (data: RegisterRequest) => http.post<User>('/api/v1/auth/register', data),
}
