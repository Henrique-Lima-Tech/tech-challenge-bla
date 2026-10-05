import { env } from '../config/env'
import { ApiError, defaultMessage } from './apiError'
import { tokenStorage } from './tokenStorage'
import type { ProblemDetail } from './types'

type QueryParams = Record<string, string | number | undefined>

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  query?: QueryParams
}

let unauthorizedHandler: (() => void) | null = null

/**
 * Called when a request sent with a token gets a 401: the session expired or was rejected.
 */
export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler
}

/**
 * Joins `VITE_API_URL` and the path. An empty `VITE_API_URL` means the page's own origin, where
 * Vite or Nginx forwards `/api`. Empty query values are left out.
 */
function buildUrl(path: string, query?: QueryParams): string {
  const base = env.apiUrl.endsWith('/') ? env.apiUrl.slice(0, -1) : env.apiUrl
  const url = new URL(base + path, globalThis.location.origin)
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== '') url.searchParams.set(key, String(value))
  }
  return url.toString()
}

/**
 * Reads the `ProblemDetail` body. A body that is not JSON, such as a proxy's HTML error page,
 * gets the default message for the status.
 */
async function readProblem(response: Response): Promise<ProblemDetail> {
  try {
    const body = (await response.json()) as Partial<ProblemDetail>
    return { ...body, status: response.status }
  } catch {
    return { status: response.status, detail: defaultMessage(response.status) }
  }
}

async function request<T>(
  path: string,
  { method = 'GET', body, query }: RequestOptions = {},
): Promise<T> {
  const token = tokenStorage.get()
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`

  let response: Response
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    // fetch only rejects when the server cannot be reached.
    throw new ApiError(0, defaultMessage(0))
  }

  if (!response.ok) {
    // A 401 without a token is a failed login, not an expired session.
    if (response.status === 401 && token) unauthorizedHandler?.()
    throw ApiError.fromProblem(await readProblem(response))
  }

  // A 204 (DELETE) has no body to parse.
  const text = await response.text()
  return (text === '' ? undefined : JSON.parse(text)) as T
}

/**
 * The only way the app calls the back end: it sends the token and turns every error response
 * into an `ApiError`.
 */
export const http = {
  get: <T>(path: string, query?: QueryParams) => request<T>(path, { query }),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body }),
  put: <T>(path: string, body: unknown) => request<T>(path, { method: 'PUT', body }),
  delete: (path: string) => request<void>(path, { method: 'DELETE' }),
}
