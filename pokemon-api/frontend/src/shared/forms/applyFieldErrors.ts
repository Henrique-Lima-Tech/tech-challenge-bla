import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import type { FieldError } from '../api/types'

/**
 * Shows the server's field errors under the matching inputs and focuses the first one.
 * A nested field such as `abilities[0]` goes to its top-level input.
 *
 * @returns the errors that match no input, to be shown some other way
 */
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
