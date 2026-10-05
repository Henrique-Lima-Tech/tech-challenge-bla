import { describe, expect, it } from 'vitest'
import { fromState, readFrom } from './navigationState'

describe('navigationState', () => {
  it('saves the current path and query as the place to come back to', () => {
    expect(fromState({ pathname: '/my-pokemon', search: '?page=2' })).toEqual({
      from: '/my-pokemon?page=2',
    })
  })

  it('reads only internal paths and ignores anything else', () => {
    expect(readFrom({ from: '/pokemon?page=3' })).toBe('/pokemon?page=3')
    for (const invalid of [
      null,
      undefined,
      'x',
      {},
      { from: 42 },
      { from: 'https://evil.dev' },
      { from: '//evil.dev' },
    ]) {
      expect(readFrom(invalid)).toBeNull()
    }
  })
})
