import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { findMockPokemon, toDetails } from '../../../mocks/data'
import { db } from '../../../mocks/db'
import { server } from '../../../mocks/server'
import { renderApp } from '../../../test/render'

const DETAILS_URL = 'http://localhost:8080/api/v1/pokemon/:idOrName'
const CREATE_URL = 'http://localhost:8080/api/v1/local/pokemon'

describe('PokemonDetailPage', () => {
  it('shows the image, stats, description and evolution chain', async () => {
    renderApp('/pokemon/pikachu')

    expect(await screen.findByRole('heading', { level: 1, name: 'Pikachu' })).toBeInTheDocument()
    expect(document.title).toBe('Pikachu · Pokédex')
    expect(screen.getByRole('img', { name: 'Pikachu' })).toBeInTheDocument()
    expect(screen.getByText('#025')).toBeInTheDocument()
    expect(screen.getByText(/several of these Pokémon gather/)).toBeInTheDocument()
    expect(screen.getByRole('meter', { name: 'Speed' })).toHaveAttribute('aria-valuenow', '90')
    expect(screen.getByText('320')).toBeInTheDocument()

    const evolution = screen.getByRole('region', { name: 'Evolution chain' })
    expect(within(evolution).getByRole('link', { name: 'Pichu' })).toHaveAttribute(
      'href',
      '/pokemon/pichu',
    )
  })

  it('says when the Pokémon has no description', async () => {
    const raichu = findMockPokemon('raichu')
    if (!raichu) throw new Error('Raichu is missing from the mock data')
    server.use(
      http.get(DETAILS_URL, () => HttpResponse.json({ ...toDetails(raichu), description: null })),
    )
    renderApp('/pokemon/raichu')

    expect(await screen.findByText('No description.')).toBeInTheDocument()
  })

  it('shows a friendly page for an unknown Pokémon', async () => {
    renderApp('/pokemon/pikachuu')
    expect(await screen.findByRole('heading', { name: 'Pokémon not found' })).toBeInTheDocument()
    expect(screen.getByText(/"pikachuu"/)).toBeInTheDocument()
    expect(document.title).toBe('Pokémon not found · Pokédex')
  })

  it('shows an error with retry and loads the Pokémon on the second try', async () => {
    server.use(http.get(DETAILS_URL, () => HttpResponse.error(), { once: true }))
    const { user } = renderApp('/pokemon/pikachu')

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Could not connect to the server' }),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Pikachu' })).toBeInTheDocument()
  })

  it('does not show the sync action to an anonymous visitor', async () => {
    renderApp('/pokemon/pikachu')
    expect(await screen.findByRole('heading', { level: 1, name: 'Pikachu' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Add to My Pokémon' })).not.toBeInTheDocument()
  })

  it('lets a logged in user save a Pokémon to My Pokémon', async () => {
    const { user } = renderApp('/pokemon/raichu', { loggedIn: true })
    await user.click(await screen.findByRole('button', { name: 'Add to My Pokémon' }))

    expect(await screen.findByText('Raichu saved to My Pokémon')).toBeInTheDocument()
    expect(db.localPokemon.some((p) => p.name === 'raichu')).toBe(true)
    const hint = screen.getByText(/Saves a copy you can edit/)
    expect(within(hint).getByRole('link', { name: 'My Pokémon' })).toHaveAttribute(
      'href',
      '/my-pokemon',
    )
  })

  it('says when the Pokémon is already in My Pokémon (409)', async () => {
    const { user } = renderApp('/pokemon/pikachu', { loggedIn: true })
    await user.click(await screen.findByRole('button', { name: 'Add to My Pokémon' }))
    expect(await screen.findByText('Pikachu is already in My Pokémon')).toBeInTheDocument()
  })

  it('shows the API message when the sync fails', async () => {
    server.use(
      http.post(CREATE_URL, () =>
        HttpResponse.json({ status: 502, detail: 'The PokéAPI is unavailable.' }, { status: 502 }),
      ),
    )
    const { user } = renderApp('/pokemon/raichu', { loggedIn: true })
    await user.click(await screen.findByRole('button', { name: 'Add to My Pokémon' }))

    expect(await screen.findByText('The PokéAPI is unavailable.')).toBeInTheDocument()
  })

  it('shows a generic message when the sync answer cannot be read', async () => {
    server.use(http.post(CREATE_URL, () => new HttpResponse('not json', { status: 201 })))
    const { user } = renderApp('/pokemon/raichu', { loggedIn: true })
    await user.click(await screen.findByRole('button', { name: 'Add to My Pokémon' }))

    expect(await screen.findByText('Could not sync.')).toBeInTheDocument()
  })
})
