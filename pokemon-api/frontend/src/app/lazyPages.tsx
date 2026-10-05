import { lazy } from 'react'

export const LoginPage = lazy(() =>
  import('../features/auth/pages/LoginPage').then((module) => ({ default: module.LoginPage })),
)
export const RegisterPage = lazy(() =>
  import('../features/auth/pages/RegisterPage').then((module) => ({
    default: module.RegisterPage,
  })),
)
export const LocalPokemonListPage = lazy(() =>
  import('../features/local-pokemon/pages/LocalPokemonListPage').then((module) => ({
    default: module.LocalPokemonListPage,
  })),
)
export const LocalPokemonEditPage = lazy(() =>
  import('../features/local-pokemon/pages/LocalPokemonEditPage').then((module) => ({
    default: module.LocalPokemonEditPage,
  })),
)
