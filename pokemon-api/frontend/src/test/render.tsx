import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactElement } from 'react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { Toaster } from 'sonner'
import { routes } from '../app/routes'
import { AuthProvider } from '../features/auth/AuthProvider'
import { tokenStorage } from '../shared/api/tokenStorage'

type Options = {
  loggedIn?: boolean
}

function createTestQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } })
}

export function renderApp(path: string, { loggedIn = false }: Options = {}) {
  if (loggedIn) {
    tokenStorage.set('mock-token-demo@pokedex.dev')
    tokenStorage.setName('Demo')
  }
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  const result = render(
    <QueryClientProvider client={createTestQueryClient()}>
      <AuthProvider>
        <RouterProvider router={router} />
        <Toaster />
      </AuthProvider>
    </QueryClientProvider>,
  )
  return { ...result, router, user: userEvent.setup() }
}

export function renderWithRouter(ui: ReactElement) {
  const router = createMemoryRouter([{ path: '*', element: ui }])
  return render(
    <QueryClientProvider client={createTestQueryClient()}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>,
  )
}
