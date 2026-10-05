import { act, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../mocks/server'
import { tokenStorage } from '../shared/api/tokenStorage'
import { renderApp } from '../test/render'

describe('errors and toasts', () => {
  it('shows "Page not found" for an unknown route', async () => {
    renderApp('/route-that-does-not-exist')
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Page not found' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View all Pokémon' })).toHaveAttribute(
      'href',
      '/pokemon',
    )
  })

  it('redirects / to the list', async () => {
    const { router } = renderApp('/')
    expect(await screen.findByRole('heading', { level: 1, name: 'Pokémon' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/pokemon')
  })

  it('logs out with a toast when the saved token has expired', async () => {
    tokenStorage.set('mock-token-expired')
    tokenStorage.setName('Expired')
    renderApp('/my-pokemon')

    expect(
      await screen.findByText('Your session has expired. Please sign in again.'),
    ).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(tokenStorage.get()).toBeNull()
    expect(tokenStorage.getName()).toBeNull()
  })

  it('shows "Could not connect to the server" on a 5xx, as the page heading', async () => {
    server.use(
      http.get('http://localhost:8080/api/v1/pokemon/:idOrName', () =>
        HttpResponse.json({ status: 503, title: 'Service Unavailable' }, { status: 503 }),
      ),
    )
    renderApp('/pokemon/pikachu')

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: 'Could not connect to the server',
      }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })

  it('moves the focus to the main content after navigating', async () => {
    const { user } = renderApp('/pokemon')
    await user.click(await screen.findByRole('link', { name: /pikachu/i }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Pikachu' })).toBeInTheDocument()
    expect(screen.getByRole('main')).toHaveFocus()
  })

  it('keeps the focus on the main content when it is already there', async () => {
    const { user, router } = renderApp('/pokemon')
    await user.click(await screen.findByRole('link', { name: /pikachu/i }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Pikachu' })).toBeInTheDocument()

    await act(() => router.navigate('/pokemon/raichu'))

    expect(await screen.findByRole('heading', { level: 1, name: 'Raichu' })).toBeInTheDocument()
    expect(screen.getByRole('main')).toHaveFocus()
  })
})
