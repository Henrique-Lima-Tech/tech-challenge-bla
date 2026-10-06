import { http } from '../../shared/api/httpClient'
import type { Page } from '../../shared/api/types'
import type { PokemonDetails, PokemonSummary } from './types'

export const pokemonApi = {
  list: (page: number, size: number) =>
    http.get<Page<PokemonSummary>>('/api/v1/pokemon', { page, size }),

  getByIdOrName: (idOrName: string | number) =>
    http.get<PokemonDetails>(
      `/api/v1/pokemon/${encodeURIComponent(String(idOrName).trim().toLowerCase())}`,
    ),
}
