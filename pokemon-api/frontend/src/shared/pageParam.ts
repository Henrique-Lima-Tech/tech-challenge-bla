export function readPageParam(searchParams: URLSearchParams): number {
  const page = Number(searchParams.get('page'))
  return Number.isInteger(page) && page >= 1 ? page : 1
}

export function withPageParam(searchParams: URLSearchParams, apiPage: number): URLSearchParams {
  const next = new URLSearchParams(searchParams)
  if (apiPage <= 0) next.delete('page')
  else next.set('page', String(apiPage + 1))
  return next
}
