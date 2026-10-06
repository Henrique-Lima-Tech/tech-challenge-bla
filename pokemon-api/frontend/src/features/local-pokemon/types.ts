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

export type LocalPokemonCreate = {
  pokemon: string
}

export type LocalPokemonUpdate = Omit<LocalPokemon, 'id' | 'pokeApiId'>

export type LocalPokemonFilters = {
  page: number
  size: number
}
