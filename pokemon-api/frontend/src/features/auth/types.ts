export type User = {
  id: number
  name: string
  email: string
}

export type LoginRequest = {
  email: string
  password: string
}

export type RegisterRequest = {
  name: string
  email: string
  password: string
}

/**
 * @param expiresIn seconds until the token expires
 * @param name the signed-in user's name (D-32)
 */
export type TokenResponse = {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  name: string
}
