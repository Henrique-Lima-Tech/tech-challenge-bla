import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../../../test/render'

describe('ProtectedRoute', () => {
  it('sends an anonymous user to the login and back after logging in', async () => {
    const { user, router } = renderApp('/my-pokemon?page=2')

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    await user.type(screen.getByLabelText('Email'), 'demo@pokedex.dev')
    await user.type(screen.getByLabelText('Password'), 'demo1234')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('heading', { name: 'My Pokémon' })).toBeInTheDocument()
    expect(router.state.location.search).toBe('?page=2')
  })

  it('also protects the edit page', async () => {
    renderApp('/my-pokemon/1/edit')
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('lets a logged in user through', async () => {
    renderApp('/my-pokemon/1/edit', { loggedIn: true })
    expect(await screen.findByRole('heading', { name: 'Edit Bulbasaur' })).toBeInTheDocument()
  })
})
