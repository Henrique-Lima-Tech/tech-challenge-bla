import { describe, expect, it } from 'vitest'
import { ApiError, defaultMessage, isApiError } from './apiError'

describe('apiError', () => {
  it('has a default message for each status', () => {
    expect(defaultMessage(0)).toBe('Could not connect to the server.')
    expect(defaultMessage(400)).toBe('The data sent is invalid.')
    expect(defaultMessage(401)).toBe('You need to sign in to continue.')
    expect(defaultMessage(403)).toBe('You do not have permission to do this.')
    expect(defaultMessage(404)).toBe('Record not found.')
    expect(defaultMessage(409)).toBe(
      'Conflict: the record already exists or was changed by someone else.',
    )
    expect(defaultMessage(503)).toBe('The server is unavailable. Please try again in a moment.')
    expect(defaultMessage(418)).toBe('Something went wrong.')
  })

  it('builds the error from a ProblemDetail, preferring detail, then title, then the default', () => {
    const fieldErrors = [{ field: 'region', message: 'Invalid region.' }]
    const withDetail = ApiError.fromProblem({
      status: 400,
      title: 'Bad Request',
      detail: 'Invalid data.',
      errors: fieldErrors,
    })
    expect(withDetail).toMatchObject({ status: 400, message: 'Invalid data.', fieldErrors })
    expect(ApiError.fromProblem({ status: 404, title: 'Not Found' }).message).toBe('Not Found')
    expect(ApiError.fromProblem({ status: 404 })).toMatchObject({
      message: 'Record not found.',
      fieldErrors: [],
    })
  })

  it('tells an ApiError apart from other errors', () => {
    expect(isApiError(new ApiError(500, 'boom'))).toBe(true)
    expect(isApiError(new Error('boom'))).toBe(false)
  })
})
