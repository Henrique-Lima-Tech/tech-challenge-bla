import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { isApiError } from '../../shared/api/apiError'
import { localPokemonApi } from './api'
import type { LocalPokemonCreate, LocalPokemonFilters, LocalPokemonUpdate } from './types'

const localPokemonKeys = {
  all: ['local-pokemon'] as const,
  list: (filters: LocalPokemonFilters) => [...localPokemonKeys.all, 'list', filters] as const,
  details: (id: number) => [...localPokemonKeys.all, 'details', id] as const,
}

export function useLocalPokemonList(filters: LocalPokemonFilters) {
  return useQuery({
    queryKey: localPokemonKeys.list(filters),
    queryFn: () => localPokemonApi.list(filters),
    placeholderData: keepPreviousData,
  })
}

export function useLocalPokemon(id: number, enabled = true) {
  return useQuery({
    queryKey: localPokemonKeys.details(id),
    queryFn: () => localPokemonApi.get(id),
    enabled,
  })
}

function useInvalidateAfterChange() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: localPokemonKeys.all })
}

export function useCreateLocalPokemon() {
  const invalidate = useInvalidateAfterChange()
  return useMutation({
    mutationFn: (input: LocalPokemonCreate) => localPokemonApi.create(input),
    onSuccess: invalidate,
  })
}

export function useUpdateLocalPokemon(id: number) {
  const invalidate = useInvalidateAfterChange()
  return useMutation({
    mutationFn: (input: LocalPokemonUpdate) => localPokemonApi.update(id, input),
    onSuccess: invalidate,
  })
}

export function useDeleteLocalPokemon() {
  const invalidate = useInvalidateAfterChange()
  return useMutation({
    mutationFn: (id: number) => localPokemonApi.remove(id),
    onSuccess: invalidate,
    onError: (error) => {
      if (isApiError(error) && error.status === 404) return invalidate()
    },
  })
}
