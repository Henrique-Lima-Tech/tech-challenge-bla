import { http } from '../../shared/api/httpClient'
import type { Page } from '../../shared/api/types'
import type {
  LocalPokemon,
  LocalPokemonCreate,
  LocalPokemonFilters,
  LocalPokemonUpdate,
} from './types'

const BASE = '/api/v1/local/pokemon'

export const localPokemonApi = {
  list: (filters: LocalPokemonFilters) => http.get<Page<LocalPokemon>>(BASE, filters),

  get: (id: number) => http.get<LocalPokemon>(`${BASE}/${id}`),

  create: (input: LocalPokemonCreate) => http.post<LocalPokemon>(BASE, input),

  update: (id: number, input: LocalPokemonUpdate) => http.put<LocalPokemon>(`${BASE}/${id}`, input),

  remove: (id: number) => http.delete(`${BASE}/${id}`),
}
