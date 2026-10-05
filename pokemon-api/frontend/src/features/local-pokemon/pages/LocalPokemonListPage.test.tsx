import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { db, pokemonToLocal } from '../../../mocks/db'
import { server } from '../../../mocks/server'
import { renderApp } from '../../../test/render'
import { localPokemonApi } from '../api'

const LIST_URL = 'http://localhost:8080/api/v1/local/pokemon'
const DELETE_URL = 'http://localhost:8080/api/v1/local/pokemon/:id'

function useDesktopLayout() {
  vi.spyOn(globalThis, 'matchMedia').mockImplementation(
    (query) =>
      ({
        matches: true,
        media: query,
        addEventListener() {},
        removeEventListener() {},
      }) as unknown as MediaQueryList,
  )
}

function addRaichuWithoutCustomData() {
  const raichu = pokemonToLocal('raichu', 4)
  if (raichu) db.localPokemon.push({ ...raichu, userId: 1 })
}

async function deleteCharmander(user: ReturnType<typeof renderApp>['user']) {
  await user.click(await screen.findByRole('button', { name: 'Delete Charmander' }))
  await user.click(
    within(screen.getByRole('dialog', { name: 'Delete Charmander?' })).getByRole('button', {
      name: 'Delete',
    }),
  )
}

describe('LocalPokemonListPage', () => {
  afterEach(() => vi.restoreAllMocks())

  it('lists the local Pokémon with the edit and delete actions', async () => {
    renderApp('/my-pokemon', { loggedIn: true })

    expect(await screen.findByRole('link', { name: '#001 Bulbasaur' })).toBeInTheDocument()
    expect(screen.getByText('3 Pokémon saved.')).toBeInTheDocument()
    expect(screen.getByText('Bulbassauro · Kanto')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Edit Bulbasaur' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Delete Bulbasaur' })).toBeInTheDocument()
  })

  it('links the name to the PokéAPI number and the edit to the local id', async () => {
    renderApp('/my-pokemon', { loggedIn: true })

    expect(await screen.findByRole('link', { name: '#004 Charmander' })).toHaveAttribute(
      'href',
      '/pokemon/4',
    )
    expect(screen.getByRole('link', { name: 'Edit Charmander' })).toHaveAttribute(
      'href',
      '/my-pokemon/2/edit',
    )
  })

  it('uses a real table on desktop', async () => {
    useDesktopLayout()
    renderApp('/my-pokemon', { loggedIn: true })

    const table = await screen.findByRole('table')
    expect(within(table).getByRole('columnheader', { name: 'Localized name' })).toBeInTheDocument()
    expect(within(table).getAllByRole('row')).toHaveLength(4)
    expect(within(table).getByRole('link', { name: 'Edit Bulbasaur' })).toHaveAttribute(
      'href',
      '/my-pokemon/1/edit',
    )
  })

  it('asks for confirmation and deletes', async () => {
    const { user } = renderApp('/my-pokemon', { loggedIn: true })
    await user.click(await screen.findByRole('button', { name: 'Delete Charmander' }))

    const dialog = screen.getByRole('dialog', { name: 'Delete Charmander?' })
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus()
    await user.click(within(dialog).getByRole('button', { name: 'Delete' }))

    expect(await screen.findByText('Charmander deleted')).toBeInTheDocument()
    await waitFor(() =>
      expect(screen.queryByRole('link', { name: /charmander/i })).not.toBeInTheDocument(),
    )
    expect(screen.getByText('2 Pokémon saved.')).toBeInTheDocument()
  })

  it('shows the empty state with a link to the list when nothing was synced', async () => {
    db.localPokemon = []
    renderApp('/my-pokemon', { loggedIn: true })

    expect(await screen.findByText('No Pokémon saved yet')).toBeInTheDocument()
    expect(screen.getByText('Add to My Pokémon')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View all Pokémon' })).toHaveAttribute(
      'href',
      '/pokemon',
    )
  })

  it('warns and refreshes the list when the Pokémon had already been deleted (404)', async () => {
    server.use(
      http.delete('http://localhost:8080/api/v1/local/pokemon/:id', () => {
        db.localPokemon = db.localPokemon.filter((p) => p.id !== 2)
        return HttpResponse.json({ status: 404, detail: 'Not found.' }, { status: 404 })
      }),
    )
    const { user } = renderApp('/my-pokemon', { loggedIn: true })
    await user.click(await screen.findByRole('button', { name: 'Delete Charmander' }))
    await user.click(
      within(screen.getByRole('dialog', { name: 'Delete Charmander?' })).getByRole('button', {
        name: 'Delete',
      }),
    )

    expect(await screen.findByText('This Pokémon had already been deleted')).toBeInTheDocument()
    await waitFor(() =>
      expect(screen.queryByRole('link', { name: /charmander/i })).not.toBeInTheDocument(),
    )
  })

  it('shows a dash for the empty custom fields on mobile', async () => {
    addRaichuWithoutCustomData()
    renderApp('/my-pokemon', { loggedIn: true })

    const raichu = (await screen.findByRole('link', { name: '#026 Raichu' })).closest('li')
    if (!raichu) throw new Error('Raichu card not found')
    expect(raichu).toHaveTextContent('— · —')
    expect(within(raichu).queryByRole('list')).not.toBeInTheDocument()
  })

  it('shows a dash for the empty custom fields on desktop', async () => {
    useDesktopLayout()
    addRaichuWithoutCustomData()
    renderApp('/my-pokemon', { loggedIn: true })

    const raichu = (await screen.findByRole('link', { name: '#026 Raichu' })).closest('tr')
    if (!raichu) throw new Error('Raichu row not found')
    const cells = within(raichu).getAllByRole('cell')
    expect(cells[1]).toHaveTextContent('—')
    expect(cells[2]).toHaveTextContent('—')
    expect(cells[3]).toHaveTextContent('—')
  })

  it('shows an error with retry when the list cannot be loaded', async () => {
    server.use(http.get(LIST_URL, () => HttpResponse.error(), { once: true }))
    const { user } = renderApp('/my-pokemon', { loggedIn: true })

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not connect to the server')
    await user.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('link', { name: '#001 Bulbasaur' })).toBeInTheDocument()
  })

  it('offers to go back to page 1 when the page is empty', async () => {
    const { user } = renderApp('/my-pokemon?page=2', { loggedIn: true })

    expect(await screen.findByText('No Pokémon on this page')).toBeInTheDocument()
    await user.click(screen.getByRole('link', { name: 'Back to page 1' }))
    expect(await screen.findByRole('link', { name: '#001 Bulbasaur' })).toBeInTheDocument()
  })

  it('changes the page with the pagination and keeps it in the URL', async () => {
    server.use(
      http.get(LIST_URL, ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page'))
        const pokemon = pokemonToLocal(page === 0 ? 'bulbasaur' : 'ivysaur', page + 1)
        return HttpResponse.json({
          content: [pokemon],
          page,
          size: 20,
          totalElements: 21,
          totalPages: 2,
        })
      }),
    )
    const { user, router } = renderApp('/my-pokemon', { loggedIn: true })

    expect(await screen.findByText('Page 1 of 2')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /next/i }))
    expect(await screen.findByRole('link', { name: '#002 Ivysaur' })).toBeInTheDocument()
    expect(router.state.location.search).toBe('?page=2')
  })

  it('closes the confirmation without deleting', async () => {
    const { user } = renderApp('/my-pokemon', { loggedIn: true })
    await user.click(await screen.findByRole('button', { name: 'Delete Charmander' }))
    await user.click(
      within(screen.getByRole('dialog', { name: 'Delete Charmander?' })).getByRole('button', {
        name: 'Cancel',
      }),
    )

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: '#004 Charmander' })).toBeInTheDocument()
  })

  it('ignores a confirm click that arrives after the dialog closed', async () => {
    const remove = vi.spyOn(localPokemonApi, 'remove')
    const { user } = renderApp('/my-pokemon', { loggedIn: true })
    await deleteCharmander(user)
    expect(await screen.findByText('Charmander deleted')).toBeInTheDocument()
    const confirm = screen.getByRole('button', { name: 'Delete', hidden: true })
    await waitFor(() => expect(confirm).toBeEnabled())
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    await act(async () => {
      fireEvent.click(confirm)
    })

    expect(remove).toHaveBeenCalledOnce()
  })

  it('shows the API message when deleting fails', async () => {
    server.use(
      http.delete(DELETE_URL, () =>
        HttpResponse.json({ status: 503, detail: 'Try again later.' }, { status: 503 }),
      ),
    )
    const { user } = renderApp('/my-pokemon', { loggedIn: true })
    await deleteCharmander(user)

    expect(await screen.findByText('Try again later.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '#004 Charmander' })).toBeInTheDocument()
  })

  it('shows a generic message when the delete answer cannot be read', async () => {
    server.use(http.delete(DELETE_URL, () => new HttpResponse('not json', { status: 200 })))
    const { user } = renderApp('/my-pokemon', { loggedIn: true })
    await deleteCharmander(user)

    expect(await screen.findByText('Could not delete.')).toBeInTheDocument()
  })
})
