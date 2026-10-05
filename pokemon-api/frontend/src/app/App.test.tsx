import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from '../App'
import { AppProviders } from './AppProviders'

describe('App', () => {
  it('starts with the real providers and router, sending / to the list', async () => {
    render(
      <AppProviders>
        <App />
      </AppProviders>,
    )
    expect(await screen.findByRole('heading', { level: 1, name: 'Pokémon' })).toBeInTheDocument()
    expect(globalThis.location.pathname).toBe('/pokemon')
  })
})
