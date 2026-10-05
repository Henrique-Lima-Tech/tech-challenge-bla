import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderWithRouter } from '../../../test/render'
import type { EvolutionStage } from '../types'
import { EvolutionChain } from './EvolutionChain'

const SPRITES = 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/'

const stage = (
  name: string,
  evolvesTo: EvolutionStage[] = [],
  spriteUrl: string | null = null,
): EvolutionStage => ({ name, spriteUrl, evolvesTo })

describe('EvolutionChain', () => {
  it('links every stage by name except the current one', () => {
    const bulbasaurLine = stage('bulbasaur', [stage('ivysaur', [stage('venusaur')])])
    renderWithRouter(<EvolutionChain chain={bulbasaurLine} currentName="ivysaur" />)

    expect(screen.getByRole('link', { name: 'Bulbasaur' })).toHaveAttribute(
      'href',
      '/pokemon/bulbasaur',
    )
    expect(screen.getByRole('link', { name: 'Venusaur' })).toHaveAttribute(
      'href',
      '/pokemon/venusaur',
    )
    expect(screen.queryByRole('link', { name: /ivysaur/i })).not.toBeInTheDocument()
    expect(screen.getByText('Ivysaur').closest('[aria-current="page"]')).toBeInTheDocument()
    expect(screen.getAllByText('evolves into')).toHaveLength(2)
  })

  it('stacks the branches of a branched chain (Eevee)', () => {
    const eevee = stage('eevee', [stage('vaporeon'), stage('jolteon'), stage('flareon')])
    renderWithRouter(<EvolutionChain chain={eevee} currentName="eevee" />)

    const branches = within(screen.getByRole('list')).getAllByRole('listitem')
    const links = branches.map((branch) => within(branch).getByRole('link'))
    expect(links[0]).toHaveAccessibleName('Vaporeon')
    expect(links[1]).toHaveAccessibleName('Jolteon')
    expect(links[2]).toHaveAccessibleName('Flareon')
  })

  it('shows the sprite of each stage, with a placeholder when it has none', () => {
    const chain = stage(
      'bulbasaur',
      [stage('ivysaur', [stage('venusaur', [], `${SPRITES}3.png`)])],
      `${SPRITES}1.png`,
    )
    renderWithRouter(<EvolutionChain chain={chain} currentName="venusaur" />)

    const bulbasaur = screen.getByRole('link', { name: 'Bulbasaur' })
    expect(bulbasaur.querySelector('img')).toHaveAttribute('src', `${SPRITES}1.png`)
    expect(bulbasaur.querySelector('img')).toHaveAttribute('alt', '')
    expect(screen.getByRole('link', { name: 'Ivysaur' })).toHaveTextContent('?')
    const current = screen.getByText('Venusaur').closest('[aria-current="page"]')
    expect(current?.querySelector('img')).toHaveAttribute('src', `${SPRITES}3.png`)
  })

  it('says when the Pokémon does not evolve (Ditto)', () => {
    renderWithRouter(<EvolutionChain chain={stage('ditto')} currentName="ditto" />)
    expect(screen.getByText('This Pokémon does not evolve.')).toBeInTheDocument()
  })
})
