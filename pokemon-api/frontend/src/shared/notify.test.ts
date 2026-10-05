import { toast } from 'sonner'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { notify } from './notify'

describe('notify', () => {
  afterEach(() => vi.restoreAllMocks())

  it('hides an error toast after 5 seconds', () => {
    const error = vi.spyOn(toast, 'error')
    notify.error('Could not save.')
    expect(error).toHaveBeenCalledWith('Could not save.', { duration: 5000 })
  })

  it('keeps an error toast with an action open until the user closes it', () => {
    const error = vi.spyOn(toast, 'error')
    const action = { label: 'Retry', onClick: () => {} }
    notify.error('Could not save.', { action })
    expect(error).toHaveBeenCalledWith('Could not save.', { duration: Infinity, action })
  })
})
