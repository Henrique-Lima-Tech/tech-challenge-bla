import { describe, expect, it } from 'vitest'
import { ApiError } from '../shared/api/apiError'
import { createQueryClient } from './queryClient'

describe('createQueryClient', () => {
  const retry = createQueryClient().getDefaultOptions().queries?.retry
  const shouldRetry = (failureCount: number, error: Error) =>
    typeof retry === 'function' ? retry(failureCount, error) : retry

  it('does not retry client errors (4xx)', () => {
    expect(shouldRetry(0, new ApiError(404, 'Not found'))).toBe(false)
    expect(shouldRetry(0, new ApiError(400, 'Bad request'))).toBe(false)
  })

  it('retries network failures and server errors up to 2 times', () => {
    expect(shouldRetry(0, new ApiError(0, 'Offline'))).toBe(true)
    expect(shouldRetry(1, new ApiError(503, 'Unavailable'))).toBe(true)
    expect(shouldRetry(2, new ApiError(503, 'Unavailable'))).toBe(false)
  })
})
