/**
 * One entry of the paginated Pokémon list (REQ-US01): sprite, category, weight and abilities.
 */
export type PokemonSummary = {
  id: number
  name: string
  spriteUrl: string | null
  category: string | null
  weightKg: number
  abilities: string[]
}

export type PokemonStat = {
  name: string
  baseStat: number
}

/**
 * One node of an evolution chain. Branches are kept: a stage may evolve into several Pokémon.
 * `spriteUrl` is null when the sprite is unknown (D-29).
 */
export type EvolutionStage = {
  name: string
  spriteUrl: string | null
  evolvesTo: EvolutionStage[]
}

/**
 * Comprehensive data of one Pokémon (REQ-US02): image, stats, description and evolution chain.
 */
export type PokemonDetails = {
  id: number
  name: string
  imageUrl: string | null
  stats: PokemonStat[]
  description: string | null
  evolutionChain: EvolutionStage
}
