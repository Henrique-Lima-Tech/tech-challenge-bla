import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ApiError } from '../api/apiError'
import { ErrorState } from './StateMessage'

describe('ErrorState', () => {
  it('shows the API message when the server answered with an error', () => {
    render(<ErrorState error={new ApiError(403, 'You do not have permission to do this.')} />)

    expect(screen.getByRole('alert')).toHaveTextContent('Could not load')
    expect(screen.getByRole('alert')).toHaveTextContent('You do not have permission to do this.')
    expect(screen.queryByRole('button', { name: 'Try again' })).not.toBeInTheDocument()
  })

  it('shows a generic message for an unexpected error', () => {
    render(<ErrorState error={new TypeError('x is undefined')} />)

    expect(screen.getByRole('alert')).toHaveTextContent('Could not load')
    expect(screen.getByRole('alert')).toHaveTextContent('Something went wrong.')
  })
})
