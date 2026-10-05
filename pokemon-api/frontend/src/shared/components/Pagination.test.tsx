import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Pagination } from './Pagination'

describe('Pagination', () => {
  it('shows the current page counting from 1 and moves to the neighbors', async () => {
    const onPageChange = vi.fn()
    render(<Pagination page={1} totalPages={5} onPageChange={onPageChange} />)

    expect(screen.getByText('Page 2 of 5')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /previous/i }))
    await userEvent.click(screen.getByRole('button', { name: /next/i }))
    expect(onPageChange.mock.calls).toEqual([[0], [2]])
  })

  it('disables "Previous" on the first page and "Next" on the last', () => {
    const { rerender } = render(<Pagination page={0} totalPages={3} onPageChange={() => {}} />)
    expect(screen.getByRole('button', { name: /previous/i })).toBeDisabled()
    expect(screen.getByRole('button', { name: /next/i })).toBeEnabled()

    rerender(<Pagination page={2} totalPages={3} onPageChange={() => {}} />)
    expect(screen.getByRole('button', { name: /next/i })).toBeDisabled()
  })

  it('renders nothing when there is a single page', () => {
    const { container } = render(<Pagination page={0} totalPages={1} onPageChange={() => {}} />)
    expect(container).toBeEmptyDOMElement()
  })
})
