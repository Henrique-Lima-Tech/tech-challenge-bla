import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../../mocks/server'
import { renderApp } from '../../../test/render'

const REGISTER_URL = 'http://localhost:8080/api/v1/auth/register'

const MISTY = {
  name: 'Misty',
  email: 'misty@pokedex.dev',
  password: 'water123',
  confirmPassword: 'water123',
}

async function fillForm(user: ReturnType<typeof renderApp>['user'], values: typeof MISTY) {
  await user.type(await screen.findByLabelText('Name'), values.name)
  await user.type(screen.getByLabelText('Email'), values.email)
  await user.type(screen.getByLabelText('Password'), values.password)
  await user.type(screen.getByLabelText('Confirm password'), values.confirmPassword)
  await user.click(screen.getByRole('button', { name: 'Create account' }))
}

describe('RegisterPage', () => {
  it('checks that the passwords match', async () => {
    const { user } = renderApp('/register')
    await fillForm(user, { ...MISTY, confirmPassword: 'water124' })
    expect(await screen.findByText('Passwords do not match.')).toBeInTheDocument()
  })

  it('shows the 409 from the API on the email field', async () => {
    const { user } = renderApp('/register')
    await fillForm(user, { ...MISTY, email: 'demo@pokedex.dev' })

    expect(await screen.findByText('This email is already registered.')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Email')).toHaveFocus()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows field errors from a 400 on their fields', async () => {
    server.use(
      http.post(REGISTER_URL, () =>
        HttpResponse.json(
          {
            status: 400,
            detail: 'Validation failed',
            errors: [{ field: 'name', message: 'size must be at most 100' }],
          },
          { status: 400 },
        ),
      ),
    )
    const { user } = renderApp('/register')
    await fillForm(user, MISTY)

    expect(await screen.findByText('size must be at most 100')).toBeInTheDocument()
    expect(screen.getByLabelText('Name')).toHaveFocus()
  })

  it('creates the account and goes to the login with the email filled in', async () => {
    const { user, router } = renderApp('/register')
    await fillForm(user, MISTY)

    expect(await screen.findByText('Account created. Please sign in.')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(screen.getByLabelText('Email')).toHaveValue('misty@pokedex.dev')
    expect(screen.getByLabelText('Password')).toHaveFocus()
  })

  it('shows the API message when the error is not about a field', async () => {
    server.use(
      http.post(REGISTER_URL, () =>
        HttpResponse.json({ status: 503, detail: 'Try again later.' }, { status: 503 }),
      ),
    )
    const { user } = renderApp('/register')
    await fillForm(user, MISTY)

    expect(await screen.findByRole('alert')).toHaveTextContent('Try again later.')
  })

  it('shows a generic message when the API answer cannot be read', async () => {
    server.use(http.post(REGISTER_URL, () => new HttpResponse('not json', { status: 201 })))
    const { user } = renderApp('/register')
    await fillForm(user, MISTY)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Something went wrong. Please try again.',
    )
  })

  it('redirects a logged in user away from the sign up page', async () => {
    const { router } = renderApp('/register', { loggedIn: true })
    expect(await screen.findByRole('heading', { name: 'Pokémon', level: 1 })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/pokemon')
  })
})
