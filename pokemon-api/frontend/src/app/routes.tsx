import { Navigate, type RouteObject } from 'react-router'
import { ProtectedRoute } from '../features/auth/components/ProtectedRoute'
import { PokemonDetailPage } from '../features/pokemon/pages/PokemonDetailPage'
import { PokemonListPage } from '../features/pokemon/pages/PokemonListPage'
import { NotFoundPage } from '../shared/components/StatusPages'
import { Layout } from './Layout'
import { LocalPokemonEditPage, LocalPokemonListPage, LoginPage, RegisterPage } from './lazyPages'
import { RouteErrorPage } from './RouteErrorPage'

export const routes: RouteObject[] = [
  {
    element: <Layout />,
    children: [
      {
        errorElement: <RouteErrorPage />,
        children: [
          { index: true, element: <Navigate to="/pokemon" replace /> },
          { path: 'pokemon', element: <PokemonListPage /> },
          { path: 'pokemon/:idOrName', element: <PokemonDetailPage /> },
          { path: 'login', element: <LoginPage /> },
          { path: 'register', element: <RegisterPage /> },
          {
            element: <ProtectedRoute />,
            children: [
              { path: 'my-pokemon', element: <LocalPokemonListPage /> },
              { path: 'my-pokemon/:id/edit', element: <LocalPokemonEditPage /> },
            ],
          },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
]
