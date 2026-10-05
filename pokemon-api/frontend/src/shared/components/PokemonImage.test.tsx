import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { PokemonImage } from './PokemonImage'

describe('PokemonImage', () => {
  it('shows a placeholder when there is no image', () => {
    render(<PokemonImage src={null} alt="Pikachu" size={96} />)
    expect(screen.getByRole('img', { name: 'Pikachu' })).toHaveTextContent('?')
  })

  it('shows a placeholder when the image fails to load', () => {
    render(<PokemonImage src="https://img.dev/25.png" alt="Pikachu" size={96} lazy />)

    const image = screen.getByRole('img', { name: 'Pikachu' })
    expect(image).toHaveAttribute('loading', 'lazy')
    fireEvent.error(image)

    expect(screen.getByRole('img', { name: 'Pikachu' })).toHaveTextContent('?')
  })
})
