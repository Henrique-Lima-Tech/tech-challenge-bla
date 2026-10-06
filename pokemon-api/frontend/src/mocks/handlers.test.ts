import { beforeEach, describe, expect, it } from 'vitest'
import { authApi } from '../features/auth/api'
import { localPokemonApi } from '../features/local-pokemon/api'
import type { LocalPokemonUpdate } from '../features/local-pokemon/types'
import { pokemonApi } from '../features/pokemon/api'
import { http } from '../shared/api/httpClient'
import { tokenStorage } from '../shared/api/tokenStorage'

const login = () => tokenStorage.set('mock-token-demo@pokedex.dev')

const validUpdate: LocalPokemonUpdate = {
  name: 'bulbasaur',
  spriteUrl: 'https://img.dev/1.png',
  category: 'Seed Pokémon',
  weightKg: 7,
  abilities: ['overgrow'],
  localizedName: 'Bulba',
  region: 'Kanto',
  internalTags: ['starter'],
}

describe('mock: catalog (US01/US02)', () => {
  it('lists a page with the fields of the contract', async () => {
    const page = await pokemonApi.list(0, 5)
    expect(page.content).toHaveLength(5)
    expect(page.content[0]).toEqual({
      id: 1,
      name: 'bulbasaur',
      spriteUrl: expect.stringContaining('/1.png') as string,
      category: 'Seed Pokémon',
      weightKg: 6.9,
      abilities: ['overgrow', 'chlorophyll'],
    })
    expect(page).toMatchObject({ page: 0, size: 5, totalElements: 17, totalPages: 4 })
  })

  it('returns an empty page beyond the total and 400 for invalid paging', async () => {
    await expect(pokemonApi.list(99, 20)).resolves.toMatchObject({ content: [] })
    await expect(pokemonApi.list(0, 101)).rejects.toMatchObject({ status: 400 })
  })

  it('finds a Pokémon by name ignoring case and spaces, with the contract fields', async () => {
    const details = await pokemonApi.getByIdOrName('  PIKACHU ')
    expect(Object.keys(details).sort()).toEqual(
      ['description', 'evolutionChain', 'id', 'imageUrl', 'name', 'stats'].sort(),
    )
    const sprite = (id: number) =>
      `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${id}.png`
    expect(details.evolutionChain).toEqual({
      name: 'pichu',
      spriteUrl: sprite(172),
      evolvesTo: [
        {
          name: 'pikachu',
          spriteUrl: sprite(25),
          evolvesTo: [{ name: 'raichu', spriteUrl: sprite(26), evolvesTo: [] }],
        },
      ],
    })
  })

  it('returns a branched evolution chain for eevee', async () => {
    const details = await pokemonApi.getByIdOrName('eevee')
    expect(details.evolutionChain.evolvesTo.map((stage) => stage.name)).toEqual([
      'vaporeon',
      'jolteon',
      'flareon',
    ])
  })

  it('returns 404 for an unknown Pokémon and 400 for a blank one', async () => {
    await expect(pokemonApi.getByIdOrName('agumon')).rejects.toMatchObject({ status: 404 })
    await expect(pokemonApi.getByIdOrName(0)).rejects.toMatchObject({ status: 404 })
    await expect(http.get('/api/v1/pokemon/%20')).rejects.toMatchObject({ status: 400 })
  })
})

describe('mock: create (US03)', () => {
  it('requires login', async () => {
    await expect(localPokemonApi.create({ pokemon: 'ivysaur' })).rejects.toMatchObject({
      status: 401,
    })
  })

  it('creates a local copy with a new local id and empty custom fields', async () => {
    login()
    const created = await localPokemonApi.create({ pokemon: 'Ivysaur' })
    expect(created).toMatchObject({
      id: 4,
      pokeApiId: 2,
      name: 'ivysaur',
      localizedName: null,
      region: null,
      internalTags: [],
    })
  })

  it('accepts the custom fields on create', async () => {
    login()
    const created = await http.post('/api/v1/local/pokemon', {
      pokemon: 'ivysaur',
      region: 'Kanto',
      internalTags: ['grass'],
    })
    expect(created).toMatchObject({ region: 'Kanto', internalTags: ['grass'] })
  })

  it('returns 409 when the Pokémon is already local, 404 when unknown, 400 when blank', async () => {
    login()
    await expect(localPokemonApi.create({ pokemon: 'bulbasaur' })).rejects.toMatchObject({
      status: 409,
    })
    await expect(localPokemonApi.create({ pokemon: 'agumon' })).rejects.toMatchObject({
      status: 404,
    })
    await expect(localPokemonApi.create({ pokemon: ' ' })).rejects.toMatchObject({
      status: 400,
      fieldErrors: [{ field: 'pokemon', message: 'must not be blank' }],
    })
    await expect(http.post('/api/v1/local/pokemon')).rejects.toMatchObject({ status: 400 })
  })
})

describe('mock: local CRUD (US04)', () => {
  beforeEach(() => login())

  it('requires login for every local route', async () => {
    tokenStorage.clear()
    await expect(localPokemonApi.list({ page: 0, size: 20 })).rejects.toMatchObject({ status: 401 })
    await expect(localPokemonApi.get(1)).rejects.toMatchObject({ status: 401 })
    await expect(localPokemonApi.update(1, validUpdate)).rejects.toMatchObject({ status: 401 })
    await expect(localPokemonApi.remove(1)).rejects.toMatchObject({ status: 401 })
  })

  it('lists the local Pokémon and rejects invalid paging', async () => {
    const page = await localPokemonApi.list({ page: 0, size: 2 })
    expect(page).toMatchObject({ totalElements: 3, totalPages: 2 })
    await expect(localPokemonApi.list({ page: -1, size: 20 })).rejects.toMatchObject({
      status: 400,
    })
  })

  it('replaces every field except the identifiers', async () => {
    const updated = await localPokemonApi.update(1, validUpdate)
    expect(updated).toEqual({ id: 1, pokeApiId: 1, ...validUpdate })
    await expect(localPokemonApi.get(1)).resolves.toEqual(updated)
  })

  it('returns 400 with field errors for an invalid payload', async () => {
    const error = await localPokemonApi
      .update(1, { ...validUpdate, abilities: [], weightKg: 1.25 })
      .catch((e: unknown) => e)
    expect(error).toMatchObject({
      status: 400,
      fieldErrors: [
        { field: 'weightKg', message: 'Use at most 1 decimal place.' },
        { field: 'abilities', message: 'Enter at least one ability.' },
      ],
    })
  })

  it('reports the index of an invalid list item', async () => {
    const error = await localPokemonApi
      .update(1, { ...validUpdate, internalTags: ['a'.repeat(31)] })
      .catch((e: unknown) => e)
    expect(error).toMatchObject({ fieldErrors: [{ field: 'internalTags[0]' }] })
  })

  it('rejects identifiers and a missing body with 400', async () => {
    await expect(
      http.put('/api/v1/local/pokemon/1', { ...validUpdate, id: 9 }),
    ).rejects.toMatchObject({
      status: 400,
    })
    await expect(http.put('/api/v1/local/pokemon/1', null)).rejects.toMatchObject({ status: 400 })
  })

  it('returns 404 for a missing record and 400 for an invalid id', async () => {
    await expect(localPokemonApi.update(999, validUpdate)).rejects.toMatchObject({ status: 404 })
    await expect(http.get('/api/v1/local/pokemon/abc')).rejects.toMatchObject({ status: 400 })
  })

  it('deletes and then returns 404', async () => {
    await localPokemonApi.remove(2)
    await expect(localPokemonApi.get(2)).rejects.toMatchObject({ status: 404 })
    await expect(localPokemonApi.remove(2)).rejects.toMatchObject({ status: 404 })
  })
})

describe('mock: local Pokémon per user', () => {
  const loginAsMisty = async () => {
    await authApi.register({ name: 'Misty', email: 'misty@pokedex.dev', password: 'water123' })
    tokenStorage.set('mock-token-misty@pokedex.dev')
  }

  it("hides another user's Pokémon in the list and answers 404 for their ids", async () => {
    await loginAsMisty()
    await expect(localPokemonApi.list({ page: 0, size: 20 })).resolves.toMatchObject({
      content: [],
      totalElements: 0,
    })
    await expect(localPokemonApi.get(1)).rejects.toMatchObject({ status: 404 })
    await expect(localPokemonApi.update(1, validUpdate)).rejects.toMatchObject({ status: 404 })
    await expect(localPokemonApi.remove(1)).rejects.toMatchObject({ status: 404 })
  })

  it('lets another user sync a Pokémon the first one already has', async () => {
    await loginAsMisty()
    const copy = await localPokemonApi.create({ pokemon: 'bulbasaur' })
    expect(copy).toMatchObject({ pokeApiId: 1, name: 'bulbasaur' })
    await expect(localPokemonApi.create({ pokemon: 'bulbasaur' })).rejects.toMatchObject({
      status: 409,
    })
    login()
    await expect(localPokemonApi.list({ page: 0, size: 20 })).resolves.toMatchObject({
      totalElements: 3,
    })
  })
})

describe('mock: auth', () => {
  it('logs in with the demo credentials', async () => {
    await expect(
      authApi.login({ email: 'demo@pokedex.dev', password: 'demo1234' }),
    ).resolves.toEqual({
      accessToken: 'mock-token-demo@pokedex.dev',
      tokenType: 'Bearer',
      expiresIn: 3600,
      name: 'Demo',
    })
  })

  it('answers a generic 401 for wrong credentials and 400 for missing ones', async () => {
    await expect(
      authApi.login({ email: 'demo@pokedex.dev', password: 'wrong' }),
    ).rejects.toMatchObject({ status: 401, message: 'Invalid email or password.' })
    await expect(authApi.login({ email: '', password: '' })).rejects.toMatchObject({
      status: 400,
    })
  })

  it('registers a user and rejects a used email with 409', async () => {
    const data = { name: 'Misty', email: 'misty@pokedex.dev', password: 'water123' }
    await expect(authApi.register(data)).resolves.toEqual({
      id: 2,
      name: 'Misty',
      email: 'misty@pokedex.dev',
    })
    await expect(authApi.register(data)).rejects.toMatchObject({ status: 409 })
  })

  it('rejects a short password and a missing body with 400', async () => {
    const error = await authApi
      .register({ name: 'Brock', email: 'brock@pokedex.dev', password: 'rock' })
      .catch((e: unknown) => e)
    expect(error).toMatchObject({ status: 400, fieldErrors: [{ field: 'password' }] })
    await expect(http.post('/api/v1/auth/register')).rejects.toMatchObject({ status: 400 })
  })
})
