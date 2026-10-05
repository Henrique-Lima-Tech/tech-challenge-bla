import { afterEach, describe, expect, it, vi } from 'vitest'

describe('env', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
    vi.resetModules()
  })

  it('uses the local backend when VITE_API_URL is not set', async () => {
    vi.stubEnv('VITE_API_URL', undefined)
    vi.resetModules()
    const { env } = await import('./env')
    expect(env.apiUrl).toBe('http://localhost:8080')
  })

  it('turns the mocks on only when VITE_USE_MOCKS is "true"', async () => {
    vi.stubEnv('VITE_API_URL', 'https://api.pokedex.dev')
    vi.stubEnv('VITE_USE_MOCKS', 'true')
    vi.resetModules()
    const { env } = await import('./env')
    expect(env).toEqual({ apiUrl: 'https://api.pokedex.dev', useMocks: true })
  })
})
