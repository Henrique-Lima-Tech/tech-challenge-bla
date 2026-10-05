import type { EvolutionStage, PokemonDetails, PokemonSummary } from '../features/pokemon/types'

type MockPokemon = {
  id: number
  name: string
  category: string
  weightKg: number
  abilities: string[]
  stats: [number, number, number, number, number, number]
  description: string
}

const STAT_NAMES = ['hp', 'attack', 'defense', 'special-attack', 'special-defense', 'speed']

// prettier-ignore
export const MOCK_POKEMON: MockPokemon[] = [
  { id: 1, name: 'bulbasaur', category: 'Seed Pokémon', weightKg: 6.9, abilities: ['overgrow', 'chlorophyll'], stats: [45, 49, 49, 65, 65, 45], description: 'A strange seed was planted on its back at birth. The plant sprouts and grows with this Pokémon.' },
  { id: 2, name: 'ivysaur', category: 'Seed Pokémon', weightKg: 13, abilities: ['overgrow', 'chlorophyll'], stats: [60, 62, 63, 80, 80, 60], description: 'When the bulb on its back grows large, it appears to lose the ability to stand on its hind legs.' },
  { id: 3, name: 'venusaur', category: 'Seed Pokémon', weightKg: 100, abilities: ['overgrow', 'chlorophyll'], stats: [80, 82, 83, 100, 100, 80], description: 'The plant blooms when it is absorbing solar energy. It stays on the move to seek sunlight.' },
  { id: 4, name: 'charmander', category: 'Lizard Pokémon', weightKg: 8.5, abilities: ['blaze', 'solar-power'], stats: [39, 52, 43, 60, 50, 65], description: 'Obviously prefers hot places. When it rains, steam is said to spout from the tip of its tail.' },
  { id: 5, name: 'charmeleon', category: 'Flame Pokémon', weightKg: 19, abilities: ['blaze', 'solar-power'], stats: [58, 64, 58, 80, 65, 80], description: 'When it swings its burning tail, it elevates the temperature to unbearably high levels.' },
  { id: 6, name: 'charizard', category: 'Flame Pokémon', weightKg: 90.5, abilities: ['blaze', 'solar-power'], stats: [78, 84, 78, 109, 85, 100], description: 'Spits fire that is hot enough to melt boulders. Known to cause forest fires unintentionally.' },
  { id: 7, name: 'squirtle', category: 'Tiny Turtle Pokémon', weightKg: 9, abilities: ['torrent', 'rain-dish'], stats: [44, 48, 65, 50, 64, 43], description: 'After birth, its back swells and hardens into a shell. Powerfully sprays foam from its mouth.' },
  { id: 8, name: 'wartortle', category: 'Turtle Pokémon', weightKg: 22.5, abilities: ['torrent', 'rain-dish'], stats: [59, 63, 80, 65, 80, 58], description: 'Often hides in water to stalk unwary prey. For swimming fast, it moves its ears to maintain balance.' },
  { id: 9, name: 'blastoise', category: 'Shellfish Pokémon', weightKg: 85.5, abilities: ['torrent', 'rain-dish'], stats: [79, 83, 100, 85, 105, 78], description: 'A brutal Pokémon with pressurized water jets on its shell. They are used for high speed tackles.' },
  { id: 25, name: 'pikachu', category: 'Mouse Pokémon', weightKg: 6, abilities: ['static', 'lightning-rod'], stats: [35, 55, 40, 50, 50, 90], description: 'When several of these Pokémon gather, their electricity could build and cause lightning storms.' },
  { id: 26, name: 'raichu', category: 'Mouse Pokémon', weightKg: 30, abilities: ['static', 'lightning-rod'], stats: [60, 90, 55, 90, 80, 110], description: 'Its long tail serves as a ground to protect itself from its own high voltage power.' },
  { id: 132, name: 'ditto', category: 'Transform Pokémon', weightKg: 4, abilities: ['limber', 'imposter'], stats: [48, 48, 48, 48, 48, 48], description: 'Capable of copying an enemy’s genetic code to instantly transform itself into a duplicate of the enemy.' },
  { id: 133, name: 'eevee', category: 'Evolution Pokémon', weightKg: 6.5, abilities: ['run-away', 'adaptability', 'anticipation'], stats: [55, 55, 50, 45, 65, 55], description: 'Its genetic code is irregular. It may mutate if it is exposed to radiation from element stones.' },
  { id: 134, name: 'vaporeon', category: 'Bubble Jet Pokémon', weightKg: 29, abilities: ['water-absorb', 'hydration'], stats: [130, 65, 60, 110, 95, 65], description: 'Lives close to water. Its long tail is ridged with a fin which is often mistaken for a mermaid’s.' },
  { id: 135, name: 'jolteon', category: 'Lightning Pokémon', weightKg: 24.5, abilities: ['volt-absorb', 'quick-feet'], stats: [65, 65, 60, 110, 95, 130], description: 'It accumulates negative ions in the atmosphere to blast out 10000-volt lightning bolts.' },
  { id: 136, name: 'flareon', category: 'Flame Pokémon', weightKg: 25, abilities: ['flash-fire', 'guts'], stats: [65, 130, 60, 95, 110, 65], description: 'When storing thermal energy in its body, its temperature could soar to over 1600 degrees.' },
  { id: 172, name: 'pichu', category: 'Tiny Mouse Pokémon', weightKg: 2, abilities: ['static', 'lightning-rod'], stats: [20, 40, 15, 35, 35, 60], description: 'It is not yet skilled at storing electricity. It may send out a jolt if amused or startled.' },
]

type ChainSpec = [string, ChainSpec[]]

// prettier-ignore
const CHAINS: ChainSpec[] = [
  ['bulbasaur', [['ivysaur', [['venusaur', []]]]]],
  ['charmander', [['charmeleon', [['charizard', []]]]]],
  ['squirtle', [['wartortle', [['blastoise', []]]]]],
  ['pichu', [['pikachu', [['raichu', []]]]]],
  ['ditto', []],
  ['eevee', [['vaporeon', []], ['jolteon', []], ['flareon', []]]],
]

export function spriteUrl(id: number) {
  return `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${id}.png`
}

export function findMockPokemon(idOrName: string): MockPokemon | undefined {
  const key = idOrName.trim().toLowerCase()
  return MOCK_POKEMON.find((p) => String(p.id) === key || p.name === key)
}

function toEvolutionStage([name, next]: ChainSpec): EvolutionStage {
  const id = MOCK_POKEMON.find((p) => p.name === name)?.id
  return {
    name,
    spriteUrl: id === undefined ? null : spriteUrl(id),
    evolvesTo: next.map(toEvolutionStage),
  }
}

function containsName(spec: ChainSpec, name: string): boolean {
  return spec[0] === name || spec[1].some((child) => containsName(child, name))
}

export function toSummary(p: MockPokemon): PokemonSummary {
  return {
    id: p.id,
    name: p.name,
    spriteUrl: spriteUrl(p.id),
    category: p.category,
    weightKg: p.weightKg,
    abilities: p.abilities,
  }
}

export function toDetails(p: MockPokemon): PokemonDetails {
  const chain = CHAINS.find((spec) => containsName(spec, p.name)) ?? [p.name, []]
  return {
    id: p.id,
    name: p.name,
    imageUrl: spriteUrl(p.id),
    stats: p.stats.map((baseStat, i) => ({ name: STAT_NAMES[i], baseStat })),
    description: p.description,
    evolutionChain: toEvolutionStage(chain),
  }
}
