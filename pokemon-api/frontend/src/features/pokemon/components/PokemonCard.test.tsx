import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderWithRouter } from '../../../test/render'
import { PokemonCard } from './PokemonCard'

const bulbasaur = {
  id: 1,
  name: 'bulbasaur',
  spriteUrl: 'https://example.com/1.png',
  category: 'Seed Pokémon',
  weightKg: 6.9,
  abilities: ['overgrow', 'chlorophyll'],
}

describe('PokemonCard', () => {
  it('shows number, name, category, weight and abilities, linking to the detail', () => {
    renderWithRouter(<PokemonCard pokemon={bulbasaur} />)

    const link = screen.getByRole('link')
    expect(link).toHaveAttribute('href', '/pokemon/1')
    expect(link).toHaveTextContent('#001')
    expect(screen.getByRole('heading', { name: 'Bulbasaur' })).toBeInTheDocument()
    expect(link).toHaveTextContent('Seed Pokémon')
    expect(link).toHaveTextContent('6.9 kg')
    expect(screen.getByRole('list', { name: 'Abilities' })).toHaveTextContent('overgrowchlorophyll')
  })

  it('uses a lazy image with alt and fixed size', () => {
    renderWithRouter(<PokemonCard pokemon={bulbasaur} />)
    const image = screen.getByRole('img', { name: 'Bulbasaur' })
    expect(image).toHaveAttribute('loading', 'lazy')
    expect(image).toHaveAttribute('width', '96')
    expect(image).toHaveAttribute('height', '96')
  })

  it('shows a placeholder when there is no sprite', () => {
    renderWithRouter(<PokemonCard pokemon={{ ...bulbasaur, spriteUrl: null }} />)
    expect(screen.getByRole('img', { name: 'Bulbasaur' })).toHaveTextContent('?')
  })
})
