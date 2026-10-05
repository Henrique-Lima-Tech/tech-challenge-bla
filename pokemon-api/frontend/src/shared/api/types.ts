/**
 * One page of results. `page` is 0-based.
 */
export type Page<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/**
 * A field that failed validation (D-27). `field` may be nested, such as `abilities[0]`.
 */
export type FieldError = {
  field: string
  message: string
}

/**
 * Body of every error response (RFC 9457, D-17).
 */
export type ProblemDetail = {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  errors?: FieldError[]
}
