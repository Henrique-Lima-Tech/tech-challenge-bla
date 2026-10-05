import { act, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it } from 'vitest'
import { server } from '../../../mocks/server'
import { tokenStorage } from '../../../shared/api/tokenStorage'
import { env } from '../../../shared/config/env'
import { renderApp } from '../../../test/render'

const LOGIN_URL = 'http://localhost:8080/api/v1/auth/login'

async function signIn(user: ReturnType<typeof renderApp>['user'], password = 'demo1234') {
  await user.type(await screen.findByLabelText('Email'), 'demo@pokedex.dev')
  await user.type(screen.getByLabelText('Password'), password)
  await user.click(screen.getByRole('button', { name: 'Sign in' }))
}

describe('LoginPage', () => {
  const originalUseMocks = env.useMocks
  afterEach(() => {
    env.useMocks = originalUseMocks
  })

  it('shows the demo user of the mock only when the mock is on', async () => {
    env.useMocks = false
    const { unmount } = renderApp('/login')
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(screen.queryByText(/Demo user/)).not.toBeInTheDocument()
    unmount()

    env.useMocks = true
    renderApp('/login')
    expect(await screen.findByText('demo@pokedex.dev / demo1234')).toBeInTheDocument()
  })

  it('validates the fields on the client before calling the API', async () => {
    const { user } = renderApp('/login')
    await user.click(await screen.findByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Enter your email.')).toBeInTheDocument()
    expect(screen.getByText('Enter your password.')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Email')).toHaveFocus()
  })

  it('rejects an invalid email on the client', async () => {
    const { user } = renderApp('/login')
    await user.type(await screen.findByLabelText('Email'), 'demo')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByText('Enter a valid email.')).toBeInTheDocument()
  })

  it('shows the generic 401 message from the API', async () => {
    const { user } = renderApp('/login')
    await signIn(user, 'wrong')

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password.')
    expect(tokenStorage.get()).toBeNull()
  })

  it('logs in, keeps the token and shows the protected link', async () => {
    const { user, router } = renderApp('/login')
    await signIn(user)

    expect(await screen.findByRole('button', { name: 'Sign out' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'My Pokémon' })).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Pokémon', level: 1 })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/pokemon')
    expect(tokenStorage.get()).toBe('mock-token-demo@pokedex.dev')
    expect(screen.getByText('Demo')).toHaveTextContent('Signed in as Demo')
    expect(tokenStorage.getName()).toBe('Demo')
  })

  it('logs out from the header', async () => {
    const { user } = renderApp('/pokemon', { loggedIn: true })

    await user.click(await screen.findByRole('button', { name: 'Sign out' }))

    expect(await screen.findByText('You have signed out')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Sign in' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'My Pokémon' })).not.toBeInTheDocument()
    expect(screen.queryByText('Demo')).not.toBeInTheDocument()
    expect(tokenStorage.get()).toBeNull()
    expect(tokenStorage.getName()).toBeNull()
  })

  it('shows a 400 from the API on the field, without a general message', async () => {
    server.use(
      http.post(LOGIN_URL, () =>
        HttpResponse.json(
          {
            status: 400,
            detail: 'Validation failed',
            errors: [{ field: 'email', message: 'must be a well-formed email address' }],
          },
          { status: 400 },
        ),
      ),
    )
    const { user } = renderApp('/login')
    await signIn(user)

    expect(await screen.findByText('must be a well-formed email address')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveFocus()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows a generic message when the API answer cannot be read', async () => {
    server.use(http.post(LOGIN_URL, () => new HttpResponse('not json', { status: 200 })))
    const { user } = renderApp('/login')
    await signIn(user)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Something went wrong. Please try again.',
    )
  })

  it('ignores an email in the navigation state that is not text', async () => {
    const { router } = renderApp('/pokemon')
    await act(() => router.navigate('/login', { state: { email: 42 } }))

    expect(await screen.findByLabelText('Email')).toHaveValue('')
    expect(screen.getByLabelText('Email')).toHaveFocus()
  })

  it('redirects a logged in user away from the login page', async () => {
    const { router } = renderApp('/login', { loggedIn: true })
    expect(await screen.findByRole('heading', { name: 'Pokémon', level: 1 })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/pokemon')
  })
})
