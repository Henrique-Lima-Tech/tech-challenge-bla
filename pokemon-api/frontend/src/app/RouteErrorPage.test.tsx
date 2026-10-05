import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { RouteErrorPage } from './RouteErrorPage'

function Broken(): never {
  throw new Error('render failed')
}

describe('RouteErrorPage', () => {
  afterEach(() => vi.restoreAllMocks())

  it('shows a friendly page when a screen throws while rendering', async () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})
    const router = createMemoryRouter([
      { path: '/', element: <Broken />, errorElement: <RouteErrorPage /> },
    ])
    render(<RouterProvider router={router} />)

    expect(await screen.findByRole('heading', { name: 'Something went wrong' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to home' })).toHaveAttribute('href', '/pokemon')
    expect(consoleError).toHaveBeenCalled()
  })

  it('shows "Page not found" when the router has no route for the URL', async () => {
    vi.spyOn(console, 'warn').mockImplementation(() => {})
    const router = createMemoryRouter(
      [{ path: '/', element: <p>Home</p>, errorElement: <RouteErrorPage /> }],
      { initialEntries: ['/missing'] },
    )
    render(<RouterProvider router={router} />)

    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
  })
})
