/**
 * Reads the 1-based `page` search param shown to people; anything invalid reads as page 1.
 */
export function readPageParam(searchParams: URLSearchParams): number {
  const page = Number(searchParams.get('page'))
  return Number.isInteger(page) && page >= 1 ? page : 1
}

/**
 * Writes the API page as the 1-based `page` param; the first page leaves it out.
 *
 * @param searchParams the current search params, kept as they are
 * @param apiPage 0-based page number
 */
export function withPageParam(searchParams: URLSearchParams, apiPage: number): URLSearchParams {
  const next = new URLSearchParams(searchParams)
  if (apiPage <= 0) next.delete('page')
  else next.set('page', String(apiPage + 1))
  return next
}
