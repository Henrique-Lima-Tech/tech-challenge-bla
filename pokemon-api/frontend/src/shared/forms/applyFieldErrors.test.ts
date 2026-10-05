import { describe, expect, it, vi } from 'vitest'
import { applyFieldErrors } from './applyFieldErrors'

type Values = { username: string; tags: string }

describe('applyFieldErrors', () => {
  it('puts each error on its field, focuses only the first and returns the unknown ones', () => {
    const setError = vi.fn()
    const unknown = applyFieldErrors<Values>(
      [
        { field: 'username', message: 'Taken.' },
        { field: 'captcha', message: 'Required.' },
        { field: 'tags[0]', message: 'Too long.' },
      ],
      ['username', 'tags'],
      setError,
    )

    expect(setError).toHaveBeenNthCalledWith(
      1,
      'username',
      { type: 'server', message: 'Taken.' },
      { shouldFocus: true },
    )
    expect(setError).toHaveBeenNthCalledWith(
      2,
      'tags',
      { type: 'server', message: 'Too long.' },
      { shouldFocus: false },
    )
    expect(unknown).toEqual([{ field: 'captcha', message: 'Required.' }])
  })
})
