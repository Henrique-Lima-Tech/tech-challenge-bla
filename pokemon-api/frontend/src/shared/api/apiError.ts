import type { FieldError, ProblemDetail } from './types'

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: FieldError[]

  constructor(status: number, message: string, fieldErrors: FieldError[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }

  static fromProblem(problem: ProblemDetail): ApiError {
    const message = problem.detail ?? problem.title ?? defaultMessage(problem.status)
    return new ApiError(problem.status, message, problem.errors ?? [])
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

export function defaultMessage(status: number): string {
  if (status === 0) return 'Could not connect to the server.'
  if (status === 400) return 'The data sent is invalid.'
  if (status === 401) return 'You need to sign in to continue.'
  if (status === 403) return 'You do not have permission to do this.'
  if (status === 404) return 'Record not found.'
  if (status === 409) return 'Conflict: the record already exists or was changed by someone else.'
  if (status >= 500) return 'The server is unavailable. Please try again in a moment.'
  return 'Something went wrong.'
}
