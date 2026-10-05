import { renderHook } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useAuth } from './useAuth'

describe('useAuth', () => {
  afterEach(() => vi.restoreAllMocks())

  it('fails with a clear message when used outside the AuthProvider', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    expect(() => renderHook(() => useAuth())).toThrow('useAuth must be used inside <AuthProvider>')
  })
})
