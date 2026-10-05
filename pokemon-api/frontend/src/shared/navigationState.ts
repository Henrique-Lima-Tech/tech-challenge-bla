type FromState = { from: string }

/**
 * Location state that remembers the page to return to after login.
 */
export function fromState(location: { pathname: string; search: string }): FromState {
  return { from: location.pathname + location.search }
}

/**
 * History state is untrusted, so only an app path is accepted.
 *
 * @return the page to return to, or null
 */
export function readFrom(state: unknown): string | null {
  if (typeof state !== 'object' || state === null || !('from' in state)) return null
  const { from } = state
  return typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') ? from : null
}
