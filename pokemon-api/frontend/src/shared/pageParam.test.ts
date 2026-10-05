import { describe, expect, it } from 'vitest'
import { readPageParam, withPageParam } from './pageParam'

describe('pageParam', () => {
  it('reads ?page as a page starting at 1 and treats invalid values as 1', () => {
    expect(readPageParam(new URLSearchParams('page=3'))).toBe(3)
    for (const invalid of ['', 'page=abc', 'page=0', 'page=-2', 'page=1.5']) {
      expect(readPageParam(new URLSearchParams(invalid))).toBe(1)
    }
  })

  it('writes the API page (starting at 0) to the URL, leaving page 1 out and keeping other params', () => {
    expect(withPageParam(new URLSearchParams('page=2&x=1'), 0).toString()).toBe('x=1')
    expect(withPageParam(new URLSearchParams('x=1'), 2).toString()).toBe('x=1&page=3')
  })
})
