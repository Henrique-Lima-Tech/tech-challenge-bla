import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../../mocks/server'
import { renderApp } from '../../../test/render'

describe('PokemonListPage', () => {
  it('lists the first page with the total', async () => {
    renderApp('/pokemon')

    expect(screen.getByRole('status')).toHaveTextContent('Loading Pokémon...')
    expect(await screen.findByRole('heading', { name: 'Bulbasaur' })).toBeInTheDocument()
    expect(screen.getByText('17 Pokémon from the PokéAPI.')).toBeInTheDocument()
    expect(document.title).toBe('Pokémon · Pokédex')
  })

  it('reads the page from the URL and changes it with the pagination', async () => {
    const { router, user } = renderApp('/pokemon?page=2')

    expect(await screen.findByText('No Pokémon on this page')).toBeInTheDocument()
    await user.click(screen.getByRole('link', { name: 'Back to page 1' }))
    expect(await screen.findByRole('heading', { name: 'Bulbasaur' })).toBeInTheDocument()
    expect(router.state.location.search).toBe('')
  })

  it('treats an invalid page as page 1', async () => {
    renderApp('/pokemon?page=abc')
    expect(await screen.findByRole('heading', { name: 'Bulbasaur' })).toBeInTheDocument()
  })

  it('shows an error with retry when the server is down', async () => {
    server.use(
      http.get('http://localhost:8080/api/v1/pokemon', () => HttpResponse.error(), { once: true }),
    )

    const { user } = renderApp('/pokemon')
    expect(await screen.findByRole('alert')).toHaveTextContent('Could not connect to the server')
    await user.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('heading', { name: 'Bulbasaur' })).toBeInTheDocument()
  })

  it('changes the page with the pagination and keeps it in the URL', async () => {
    server.use(
      http.get('http://localhost:8080/api/v1/pokemon', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page'))
        const name = page === 0 ? 'bulbasaur' : 'ivysaur'
        return HttpResponse.json({
          content: [
            { id: page + 1, name, spriteUrl: null, category: null, weightKg: 1, abilities: [] },
          ],
          page,
          size: 20,
          totalElements: 40,
          totalPages: 2,
        })
      }),
    )
    const { user, router } = renderApp('/pokemon')

    expect(await screen.findByText('Page 1 of 2')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /next/i }))
    expect(await screen.findByRole('heading', { name: 'Ivysaur' })).toBeInTheDocument()
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument()
    expect(router.state.location.search).toBe('?page=2')
  })
})
