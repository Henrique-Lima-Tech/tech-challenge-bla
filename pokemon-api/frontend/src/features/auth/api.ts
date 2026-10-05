import { http } from '../../shared/api/httpClient'
import type { LoginRequest, RegisterRequest, TokenResponse, User } from './types'

export const authApi = {
  /**
   * @throws {ApiError} 400 if a value breaks a validation rule
   * @throws {ApiError} 401 if the email is unknown or the password is wrong
   */
  login: (credentials: LoginRequest) => http.post<TokenResponse>('/api/v1/auth/login', credentials),

  /**
   * @throws {ApiError} 400 if a value breaks a validation rule
   * @throws {ApiError} 409 if another user already has the email
   */
  register: (data: RegisterRequest) => http.post<User>('/api/v1/auth/register', data),
}
