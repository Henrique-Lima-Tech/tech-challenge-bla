type FromState = { from: string }

export function fromState(location: { pathname: string; search: string }): FromState {
  return { from: location.pathname + location.search }
}

export function readFrom(state: unknown): string | null {
  if (typeof state !== 'object' || state === null || !('from' in state)) return null
  const { from } = state
  return typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') ? from : null
}
