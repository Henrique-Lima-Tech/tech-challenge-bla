import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { tokenStorage } from '../../shared/api/tokenStorage'
import { renderApp } from '../../test/render'

describe('AuthProvider', () => {
  it('starts logged in when a token is saved, with the saved name', async () => {
    renderApp('/pokemon', { loggedIn: true })
    expect(await screen.findByRole('button', { name: 'Sign out' })).toBeInTheDocument()
    expect(screen.getByText('Demo')).toHaveTextContent('Signed in as Demo')
  })

  it('shows no name for a session saved before the name existed', async () => {
    tokenStorage.set('mock-token-demo@pokedex.dev')
    renderApp('/pokemon')
    expect(await screen.findByRole('button', { name: 'Sign out' })).toBeInTheDocument()
    expect(screen.queryByText(/Signed in as/)).not.toBeInTheDocument()
  })

  it('starts logged out without a token', async () => {
    renderApp('/pokemon')
    expect(await screen.findByRole('link', { name: 'Sign in' })).toBeInTheDocument()
    expect(tokenStorage.get()).toBeNull()
  })
})
