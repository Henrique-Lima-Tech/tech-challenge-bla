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

export type EvolutionStage = {
  name: string
  spriteUrl: string | null
  evolvesTo: EvolutionStage[]
}

export type PokemonDetails = {
  id: number
  name: string
  imageUrl: string | null
  stats: PokemonStat[]
  description: string | null
  evolutionChain: EvolutionStage
}
