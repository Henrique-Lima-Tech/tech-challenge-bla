import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ConfirmDialog } from './ConfirmDialog'

function renderDialog(props: Partial<Parameters<typeof ConfirmDialog>[0]> = {}) {
  const handlers = { onConfirm: vi.fn(), onCancel: vi.fn() }
  render(
    <ConfirmDialog open title="Delete Bulbasaur?" confirmLabel="Delete" {...handlers} {...props}>
      <p>The local record will be deleted.</p>
    </ConfirmDialog>,
  )
  return handlers
}

describe('ConfirmDialog', () => {
  it('opens as a modal with the focus on "Cancel"', () => {
    renderDialog()
    expect(screen.getByRole('dialog', { name: 'Delete Bulbasaur?' })).toBeVisible()
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus()
  })

  it('calls onConfirm and onCancel', async () => {
    const { onConfirm, onCancel } = renderDialog()
    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(onConfirm).toHaveBeenCalledOnce()
    expect(onCancel).toHaveBeenCalledOnce()
  })

  it('disables both actions while pending', () => {
    renderDialog({ pending: true })
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Delete' })).toBeDisabled()
  })

  it('closes with Esc', () => {
    const { onCancel } = renderDialog()
    const escape = new Event('cancel', { cancelable: true })
    fireEvent(screen.getByRole('dialog'), escape)
    expect(escape.defaultPrevented).toBe(true)
    expect(onCancel).toHaveBeenCalledOnce()
  })

  it('ignores Esc while the action is pending', () => {
    const { onCancel } = renderDialog({ pending: true })
    fireEvent(screen.getByRole('dialog'), new Event('cancel', { cancelable: true }))
    expect(onCancel).not.toHaveBeenCalled()
  })

  it('tells the parent when the browser closes the dialog on its own', () => {
    const { onCancel } = renderDialog()
    fireEvent(screen.getByRole('dialog'), new Event('close'))
    expect(onCancel).toHaveBeenCalledOnce()
  })

  it('closes when the parent sets open to false, without calling onCancel', () => {
    const handlers = { onConfirm: vi.fn(), onCancel: vi.fn() }
    const dialog = (open: boolean) => (
      <ConfirmDialog open={open} title="Delete Bulbasaur?" confirmLabel="Delete" {...handlers}>
        <p>The local record will be deleted.</p>
      </ConfirmDialog>
    )
    const { rerender } = render(dialog(true))
    rerender(dialog(false))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(handlers.onCancel).not.toHaveBeenCalled()
  })
})
