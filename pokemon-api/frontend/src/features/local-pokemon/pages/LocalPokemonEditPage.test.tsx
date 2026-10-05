import { screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { db, pokemonToLocal } from '../../../mocks/db'
import { server } from '../../../mocks/server'
import { renderApp } from '../../../test/render'

const LOCAL_URL = 'http://localhost:8080/api/v1/local/pokemon/:id'

describe('LocalPokemonEditPage', () => {
  it('loads every field and keeps "Save changes" disabled until something changes', async () => {
    renderApp('/my-pokemon/1/edit', { loggedIn: true })

    expect(await screen.findByLabelText('Name')).toHaveValue('bulbasaur')
    expect(screen.getByLabelText('Category')).toHaveValue('Seed Pokémon')
    expect(screen.getByLabelText('Weight (kg)')).toHaveValue('6.9')
    expect(screen.getByLabelText('Sprite URL')).toHaveValue(
      'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png',
    )
    expect(screen.getByLabelText('Abilities (comma separated)')).toHaveValue(
      'overgrow, chlorophyll',
    )
    expect(screen.getByLabelText('Localized name')).toHaveValue('Bulbassauro')
    expect(screen.getByLabelText('Region')).toHaveValue('Kanto')
    expect(screen.getByLabelText('Tags (comma separated)')).toHaveValue('starter, grass')
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeDisabled()
    expect(screen.getByRole('link', { name: 'View details' })).toHaveAttribute('href', '/pokemon/1')
  })

  it('previews the normalized tags and counts the abilities', async () => {
    const { user } = renderApp('/my-pokemon/1/edit', { loggedIn: true })
    const tags = await screen.findByLabelText('Tags (comma separated)')
    await user.clear(tags)
    await user.type(tags, 'Starter, KANTO , starter')

    expect(screen.getByRole('list', { name: 'Tags preview' })).toHaveTextContent('starterkanto')
    expect(screen.getByText('2/20')).toBeInTheDocument()
    expect(screen.getByText('2/10')).toBeInTheDocument()
  })

  it('validates on the client before calling the API', async () => {
    const { user } = renderApp('/my-pokemon/1/edit', { loggedIn: true })
    const weight = await screen.findByLabelText('Weight (kg)')
    await user.clear(weight)
    await user.type(weight, '6.95')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('Use at most 1 decimal place.')).toBeInTheDocument()
    expect(weight).toHaveFocus()
  })

  it('saves every field and goes back to the list', async () => {
    const { user, router } = renderApp('/my-pokemon/1/edit', { loggedIn: true })
    const localizedName = await screen.findByLabelText('Localized name')
    await user.clear(localizedName)
    await user.type(localizedName, 'Bulba')
    const weight = screen.getByLabelText('Weight (kg)')
    await user.clear(weight)
    await user.type(weight, '7.5')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('Changes saved')).toBeInTheDocument()
    await waitFor(() => expect(router.state.location.pathname).toBe('/my-pokemon'))
    expect(db.localPokemon.find((p) => p.id === 1)).toMatchObject({
      localizedName: 'Bulba',
      weightKg: 7.5,
      abilities: ['overgrow', 'chlorophyll'],
    })
  })

  it('shows a 400 from the API on the right field, including list items', async () => {
    server.use(
      http.put(LOCAL_URL, () =>
        HttpResponse.json(
          {
            status: 400,
            title: 'Bad Request',
            detail: 'Validation failed',
            errors: [{ field: 'abilities[0]', message: 'size must be at most 50' }],
          },
          { status: 400 },
        ),
      ),
    )
    const { user } = renderApp('/my-pokemon/1/edit', { loggedIn: true })
    const abilities = await screen.findByLabelText('Abilities (comma separated)')
    await user.type(abilities, '!')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('size must be at most 50')).toBeInTheDocument()
    expect(abilities).toHaveAttribute('aria-invalid', 'true')
    expect(abilities).toHaveFocus()
  })

  it('starts with empty custom fields when the Pokémon has none yet', async () => {
    const raichu = pokemonToLocal('raichu', 4)
    if (raichu) db.localPokemon.push({ ...raichu, userId: 1 })
    renderApp('/my-pokemon/4/edit', { loggedIn: true })

    expect(await screen.findByLabelText('Localized name')).toHaveValue('')
    expect(screen.getByLabelText('Region')).toHaveValue('')
    expect(screen.getByLabelText('Tags (comma separated)')).toHaveValue('')
  })

  it('shows empty inputs for a missing sprite URL and category', async () => {
    const stored = db.localPokemon.find((p) => p.id === 1)
    if (!stored) throw new Error('Bulbasaur is missing from the mock database')
    Object.assign(stored, { spriteUrl: null, category: null })
    renderApp('/my-pokemon/1/edit', { loggedIn: true })

    expect(await screen.findByLabelText('Sprite URL')).toHaveValue('')
    expect(screen.getByLabelText('Category')).toHaveValue('')
  })

  it('shows an error with retry when loading fails', async () => {
    server.use(
      http.get(
        LOCAL_URL,
        () => HttpResponse.json({ status: 503, detail: 'Try again later.' }, { status: 503 }),
        { once: true },
      ),
    )
    const { user } = renderApp('/my-pokemon/1/edit', { loggedIn: true })

    expect(await screen.findByRole('alert')).toHaveTextContent('Try again later.')
    await user.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByLabelText('Localized name')).toHaveValue('Bulbassauro')
  })

  it('shows a toast when saving fails without a field error', async () => {
    server.use(
      http.put(LOCAL_URL, () =>
        HttpResponse.json({ status: 503, detail: 'Try again later.' }, { status: 503 }),
      ),
    )
    const { user } = renderApp('/my-pokemon/1/edit', { loggedIn: true })
    await user.type(await screen.findByLabelText('Localized name'), '!')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('Try again later.')).toBeInTheDocument()
  })

  it('shows a generic toast when the save answer cannot be read', async () => {
    server.use(http.put(LOCAL_URL, () => new HttpResponse('not json', { status: 200 })))
    const { user } = renderApp('/my-pokemon/1/edit', { loggedIn: true })
    await user.type(await screen.findByLabelText('Localized name'), '!')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('Could not save.')).toBeInTheDocument()
  })

  it('shows "Pokémon not found" for an id that is not in My Pokémon', async () => {
    renderApp('/my-pokemon/999/edit', { loggedIn: true })
    expect(await screen.findByRole('heading', { name: 'Pokémon not found' })).toBeInTheDocument()
    expect(screen.getByText('This Pokémon is not in My Pokémon.')).toBeInTheDocument()
  })

  it('shows "Pokémon not found" for an invalid id without calling the API', async () => {
    renderApp('/my-pokemon/abc/edit', { loggedIn: true })
    expect(await screen.findByRole('heading', { name: 'Pokémon not found' })).toBeInTheDocument()
  })
})
