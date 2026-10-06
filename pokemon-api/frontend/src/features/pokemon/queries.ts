import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { pokemonApi } from './api'

const pokemonKeys = {
  all: ['pokemon'] as const,
  list: (page: number, size: number) => [...pokemonKeys.all, 'list', { page, size }] as const,
  details: (idOrName: string) => [...pokemonKeys.all, 'details', idOrName.toLowerCase()] as const,
}

export function usePokemonList(page: number, size = 20) {
  return useQuery({
    queryKey: pokemonKeys.list(page, size),
    queryFn: () => pokemonApi.list(page, size),
    placeholderData: keepPreviousData,
  })
}

export function usePokemonDetails(idOrName: string) {
  return useQuery({
    queryKey: pokemonKeys.details(idOrName),
    queryFn: () => pokemonApi.getByIdOrName(idOrName),
    enabled: idOrName.trim() !== '',
  })
}
