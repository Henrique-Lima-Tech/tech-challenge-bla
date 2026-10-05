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
  /**
   * @throws {ApiError} 400 if `page` or `size` is out of range
   */
  list: (filters: LocalPokemonFilters) => http.get<Page<LocalPokemon>>(BASE, filters),

  /**
   * @throws {ApiError} 400 if `id` is not a positive number
   * @throws {ApiError} 404 if the user has no copy with that id
   */
  get: (id: number) => http.get<LocalPokemon>(`${BASE}/${id}`),

  /**
   * REQ-US03: copies a Pokémon from the PokéAPI into the logged-in user's local copies (D-31).
   *
   * @throws {ApiError} 400 if a value breaks a validation rule
   * @throws {ApiError} 404 if the PokéAPI does not know it
   * @throws {ApiError} 409 if the user already has it
   * @throws {ApiError} 502 if the PokéAPI cannot answer
   */
  create: (input: LocalPokemonCreate) => http.post<LocalPokemon>(BASE, input),

  /**
   * REQ-US04: replaces every field of a local copy except its identifiers (D-25).
   *
   * @throws {ApiError} 400 if a value breaks a validation rule (D-27)
   * @throws {ApiError} 404 if the user has no copy with that id
   */
  update: (id: number, input: LocalPokemonUpdate) => http.put<LocalPokemon>(`${BASE}/${id}`, input),

  /**
   * @throws {ApiError} 400 if `id` is not a positive number
   * @throws {ApiError} 404 if the user has no copy with that id
   */
  remove: (id: number) => http.delete(`${BASE}/${id}`),
}
