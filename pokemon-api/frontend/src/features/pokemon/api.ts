import { http } from '../../shared/api/httpClient'
import type { Page } from '../../shared/api/types'
import type { PokemonDetails, PokemonSummary } from './types'

export const pokemonApi = {
  /**
   * @param page 0-based page number
   * @param size items per page, from 1 to 100
   * @throws {ApiError} 400 if `page` or `size` is out of range
   * @throws {ApiError} 502 if the PokéAPI cannot answer
   */
  list: (page: number, size: number) =>
    http.get<Page<PokemonSummary>>('/api/v1/pokemon', { page, size }),

  /**
   * @throws {ApiError} 400 if `idOrName` is blank
   * @throws {ApiError} 404 if the PokéAPI does not know it
   * @throws {ApiError} 502 if the PokéAPI cannot answer
   */
  getByIdOrName: (idOrName: string | number) =>
    http.get<PokemonDetails>(
      `/api/v1/pokemon/${encodeURIComponent(String(idOrName).trim().toLowerCase())}`,
    ),
}
