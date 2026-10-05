import { http as mswHttp, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { server } from '../../mocks/server'
import { env } from '../config/env'
import { ApiError } from './apiError'
import { http, setUnauthorizedHandler } from './httpClient'
import { tokenStorage } from './tokenStorage'

describe('httpClient', () => {
  afterEach(() => setUnauthorizedHandler(null))

  it('returns the parsed JSON body on success', async () => {
    const page = await http.get<{ page: number; size: number }>('/api/v1/pokemon', {
      page: 0,
      size: 5,
    })
    expect(page).toMatchObject({ page: 0, size: 5 })
  })

  it('sends the saved token in the Authorization header', async () => {
    tokenStorage.set('mock-token-demo@pokedex.dev')
    const page = await http.get<{ totalElements: number }>('/api/v1/local/pokemon')
    expect(page.totalElements).toBe(3)
  })

  it('throws ApiError with status and message from ProblemDetail', async () => {
    const error = await http.get('/api/v1/pokemon/missingno').catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 404, message: 'Pokémon not found.' })
  })

  it('exposes field errors from a 400 response', async () => {
    const error = await http.get('/api/v1/pokemon', { page: -1 }).catch((e: unknown) => e)
    expect(error).toMatchObject({ status: 400 })
    expect((error as ApiError).fieldErrors).toEqual([
      { field: 'page', message: 'must be greater than or equal to 0' },
    ])
  })

  it('calls the unauthorized handler when an authenticated request gets 401', async () => {
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
    tokenStorage.set('mock-token-expired')

    await expect(http.get('/api/v1/local/pokemon')).rejects.toMatchObject({ status: 401 })
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('does not call the unauthorized handler on 401 without a token (wrong login)', async () => {
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)

    await expect(
      http.post('/api/v1/auth/login', { email: 'demo@pokedex.dev', password: 'wrong' }),
    ).rejects.toMatchObject({ status: 401 })
    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('returns undefined for 204 No Content', async () => {
    tokenStorage.set('mock-token-demo@pokedex.dev')
    await expect(http.delete('/api/v1/local/pokemon/1')).resolves.toBeUndefined()
  })

  it('throws ApiError with status 0 when the server cannot be reached', async () => {
    server.use(mswHttp.get('http://localhost:8080/api/v1/pokemon', () => HttpResponse.error()))
    await expect(http.get('/api/v1/pokemon')).rejects.toMatchObject({
      status: 0,
      message: 'Could not connect to the server.',
    })
  })

  it('leaves empty query values out of the URL', async () => {
    let url = ''
    server.use(
      mswHttp.get('http://localhost:8080/api/v1/pokemon', ({ request }) => {
        url = request.url
        return HttpResponse.json({})
      }),
    )
    await http.get('/api/v1/pokemon', { page: 0, size: undefined, name: '' })
    expect(url).toBe('http://localhost:8080/api/v1/pokemon?page=0')
  })

  it('uses the default message when the error body is not JSON', async () => {
    server.use(
      mswHttp.get(
        'http://localhost:8080/api/v1/pokemon',
        () => new HttpResponse('<html lang="en">Bad Gateway</html>', { status: 502 }),
      ),
    )
    await expect(http.get('/api/v1/pokemon')).rejects.toMatchObject({
      status: 502,
      message: 'The server is unavailable. Please try again in a moment.',
    })
  })

  it('returns undefined for a success response without body', async () => {
    tokenStorage.set('mock-token-demo@pokedex.dev')
    server.use(
      mswHttp.post(
        'http://localhost:8080/api/v1/local/pokemon',
        () => new HttpResponse(null, { status: 200 }),
      ),
    )
    await expect(
      http.post('/api/v1/local/pokemon', { pokemon: 'ivysaur' }),
    ).resolves.toBeUndefined()
  })
})

describe('httpClient base URL (VITE_API_URL)', () => {
  const originalApiUrl = env.apiUrl
  afterEach(() => {
    env.apiUrl = originalApiUrl
  })

  it('calls the same origin when the API URL is empty (Nginx proxy in Docker)', async () => {
    env.apiUrl = ''
    server.use(
      mswHttp.get(`${globalThis.location.origin}/api/v1/pokemon`, () =>
        HttpResponse.json({ source: 'same-origin' }),
      ),
    )
    await expect(http.get('/api/v1/pokemon')).resolves.toEqual({ source: 'same-origin' })
  })

  it('keeps a path prefix in the API URL', async () => {
    env.apiUrl = 'http://localhost:8080/backend/'
    server.use(
      mswHttp.get('http://localhost:8080/backend/api/v1/pokemon', () =>
        HttpResponse.json({ source: 'prefixed' }),
      ),
    )
    await expect(http.get('/api/v1/pokemon')).resolves.toEqual({ source: 'prefixed' })
  })
})
