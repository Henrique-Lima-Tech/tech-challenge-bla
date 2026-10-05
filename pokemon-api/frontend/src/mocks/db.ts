import type { User } from '../features/auth/types'
import type { LocalPokemon } from '../features/local-pokemon/types'
import { findMockPokemon, spriteUrl } from './data'

export type MockUser = User & { password: string }

/**
 * A local Pokémon and the user who synced it (D-31). The owner never goes into a response.
 */
export type MockLocalPokemon = LocalPokemon & { userId: number }

type MockDb = {
  localPokemon: MockLocalPokemon[]
  users: MockUser[]
  nextLocalId: number
  nextUserId: number
}

export function pokemonToLocal(idOrName: string, id: number): LocalPokemon | null {
  const p = findMockPokemon(idOrName)
  if (!p) return null
  return {
    id,
    pokeApiId: p.id,
    name: p.name,
    spriteUrl: spriteUrl(p.id),
    category: p.category,
    weightKg: p.weightKg,
    abilities: p.abilities,
    localizedName: null,
    region: null,
    internalTags: [],
  }
}

function seedPokemon(
  idOrName: string,
  id: number,
  custom: Partial<LocalPokemon>,
): MockLocalPokemon {
  const base = pokemonToLocal(idOrName, id)
  if (!base) throw new Error(`Pokémon "${idOrName}" does not exist in data.ts`)
  return { ...base, ...custom, userId: DEMO_USER_ID }
}

const DEMO_USER_ID = 1

function createInitialState(): MockDb {
  return {
    localPokemon: [
      seedPokemon('bulbasaur', 1, {
        localizedName: 'Bulbassauro',
        region: 'Kanto',
        internalTags: ['starter', 'grass'],
      }),
      seedPokemon('charmander', 2, { region: 'Kanto', internalTags: ['starter'] }),
      seedPokemon('pikachu', 3, {
        localizedName: 'ピカチュウ',
        region: 'Kanto',
        internalTags: ['anime', 'mascot'],
      }),
    ],
    users: [{ id: DEMO_USER_ID, name: 'Demo', email: 'demo@pokedex.dev', password: 'demo1234' }],
    nextLocalId: 4,
    nextUserId: 2,
  }
}

export const db: MockDb = createInitialState()

export function resetDb() {
  Object.assign(db, createInitialState())
}

export function toPublicLocal({ userId: _owner, ...pokemon }: MockLocalPokemon): LocalPokemon {
  return pokemon
}

export function toPublicUser(user: MockUser): User {
  return { id: user.id, name: user.name, email: user.email }
}
