/**
 * A local copy of a Pokémon (REQ-US03): the PokéAPI fields plus the proprietary `localizedName`,
 * `region` and `internalTags`.
 */
export type LocalPokemon = {
  id: number
  pokeApiId: number
  name: string
  spriteUrl: string | null
  category: string | null
  weightKg: number
  abilities: string[]
  localizedName: string | null
  region: string | null
  internalTags: string[]
}

/**
 * @param pokemon PokéAPI id or name of the Pokémon to copy
 */
export type LocalPokemonCreate = {
  pokemon: string
}

/**
 * Every field a PUT replaces: all except the identifiers `id` and `pokeApiId` (D-25).
 */
export type LocalPokemonUpdate = Omit<LocalPokemon, 'id' | 'pokeApiId'>

/**
 * `page` is 0-based; `size` goes from 1 to 100 (D-26).
 */
export type LocalPokemonFilters = {
  page: number
  size: number
}
