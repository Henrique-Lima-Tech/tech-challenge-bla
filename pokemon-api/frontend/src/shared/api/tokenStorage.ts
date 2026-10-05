const TOKEN_KEY = 'pokedex.accessToken'
const NAME_KEY = 'pokedex.userName'

function read(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function write(key: string, value: string): boolean {
  try {
    localStorage.setItem(key, value)
    return true
  } catch {
    return false
  }
}

/**
 * The JWT and the user's name from the login response (D-32). A browser that blocks `localStorage`
 * does not throw: the getters return null, and the setters and `clear` return false.
 */
export const tokenStorage = {
  get: (): string | null => read(TOKEN_KEY),
  set: (token: string): boolean => write(TOKEN_KEY, token),
  getName: (): string | null => read(NAME_KEY),
  setName: (name: string): boolean => write(NAME_KEY, name),
  clear(): boolean {
    try {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(NAME_KEY)
      return true
    } catch {
      return false
    }
  },
}
