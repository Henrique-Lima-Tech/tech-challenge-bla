import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { TextField } from './Field'

describe('Field', () => {
  it('links the label, hint and error to the input', () => {
    render(<TextField label="Username" hint="3 to 30 characters." error="Taken." />)

    const input = screen.getByLabelText('Username')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAccessibleDescription('3 to 30 characters. Taken.')
  })

  it('can hide the label visually and keep it for screen readers', () => {
    render(<TextField label="Region" hideLabel />)

    expect(screen.getByLabelText('Region')).toBeInTheDocument()
    expect(screen.getByText('Region')).toHaveClass('visually-hidden')
  })
})
