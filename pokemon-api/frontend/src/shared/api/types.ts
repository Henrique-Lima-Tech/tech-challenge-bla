export type Page<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type FieldError = {
  field: string
  message: string
}

export type ProblemDetail = {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  errors?: FieldError[]
}
