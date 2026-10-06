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

export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler
}

function buildUrl(path: string, query?: QueryParams): string {
  const base = env.apiUrl.endsWith('/') ? env.apiUrl.slice(0, -1) : env.apiUrl
  const url = new URL(base + path, globalThis.location.origin)
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== '') url.searchParams.set(key, String(value))
  }
  return url.toString()
}

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
    throw new ApiError(0, defaultMessage(0))
  }

  if (!response.ok) {
    if (response.status === 401 && token) unauthorizedHandler?.()
    throw ApiError.fromProblem(await readProblem(response))
  }

  const text = await response.text()
  return (text === '' ? undefined : JSON.parse(text)) as T
}

export const http = {
  get: <T>(path: string, query?: QueryParams) => request<T>(path, { query }),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body }),
  put: <T>(path: string, body: unknown) => request<T>(path, { method: 'PUT', body }),
  delete: (path: string) => request<void>(path, { method: 'DELETE' }),
}
