import { afterEach, describe, expect, it, vi } from 'vitest'
import { tokenStorage } from './tokenStorage'

describe('tokenStorage', () => {
  afterEach(() => vi.restoreAllMocks())

  it('saves, reads and clears the token', () => {
    expect(tokenStorage.set('abc')).toBe(true)
    expect(tokenStorage.get()).toBe('abc')
    expect(tokenStorage.clear()).toBe(true)
    expect(tokenStorage.get()).toBeNull()
  })

  it('saves and reads the user name, and clear removes it with the token', () => {
    tokenStorage.set('abc')
    expect(tokenStorage.setName('Ash')).toBe(true)
    expect(tokenStorage.getName()).toBe('Ash')
    tokenStorage.clear()
    expect(tokenStorage.getName()).toBeNull()
  })

  it('does not break when the browser blocks localStorage', () => {
    const blocked = () => {
      throw new DOMException('Blocked', 'SecurityError')
    }
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(blocked)
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(blocked)
    vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(blocked)

    expect(tokenStorage.get()).toBeNull()
    expect(tokenStorage.set('abc')).toBe(false)
    expect(tokenStorage.getName()).toBeNull()
    expect(tokenStorage.setName('Ash')).toBe(false)
    expect(tokenStorage.clear()).toBe(false)
  })
})
