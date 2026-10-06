import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import type { FieldError } from '../api/types'

export function applyFieldErrors<T extends FieldValues>(
  fieldErrors: FieldError[],
  fields: readonly Path<T>[],
  setError: UseFormSetError<T>,
): FieldError[] {
  const unknown: FieldError[] = []
  let focused = false
  for (const error of fieldErrors) {
    const field = error.field.split(/[.[]/)[0] as Path<T>
    if (!fields.includes(field)) {
      unknown.push(error)
      continue
    }
    setError(field, { type: 'server', message: error.message }, { shouldFocus: !focused })
    focused = true
  }
  return unknown
}
