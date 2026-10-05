import { describe, expect, it } from 'vitest'
import { registerSchema } from '../auth/schemas'
import { localPokemonFormSchema, localPokemonUpdateSchema, parseList } from './schemas'

describe('parseList', () => {
  it('normalizes: trims, lowercases, drops empty and duplicated items', () => {
    expect(parseList('Starter, Kanto , starter,, ')).toEqual(['starter', 'kanto'])
  })
})

describe('localPokemonFormSchema', () => {
  const valid = {
    name: ' bulbasaur ',
    spriteUrl: 'https://img.dev/1.png',
    category: 'Seed Pokémon',
    weightKg: '6.9',
    abilities: 'Overgrow, chlorophyll',
    localizedName: 'Bulbassauro',
    region: 'Kanto',
    internalTags: 'Starter, grass',
  }

  it('turns the form text into the PUT body', () => {
    expect(localPokemonFormSchema.parse(valid)).toEqual({
      name: 'bulbasaur',
      spriteUrl: 'https://img.dev/1.png',
      category: 'Seed Pokémon',
      weightKg: 6.9,
      abilities: ['overgrow', 'chlorophyll'],
      localizedName: 'Bulbassauro',
      region: 'Kanto',
      internalTags: ['starter', 'grass'],
    })
  })

  it('sends empty optional fields as null and no tags as an empty list', () => {
    const parsed = localPokemonFormSchema.parse({
      ...valid,
      spriteUrl: ' ',
      category: '',
      localizedName: '  ',
      region: '',
      internalTags: '',
    })
    expect(parsed).toMatchObject({
      spriteUrl: null,
      category: null,
      localizedName: null,
      region: null,
      internalTags: [],
    })
  })

  it.each([
    ['name', { name: '  ' }, 'Enter the name.'],
    ['name', { name: 'a'.repeat(51) }, 'Use at most 50 characters.'],
    ['spriteUrl', { spriteUrl: 'ftp://img.dev/1.png' }, 'Enter a valid http or https URL.'],
    ['category', { category: 'a'.repeat(51) }, 'Use at most 50 characters.'],
    ['weightKg', { weightKg: '' }, 'Enter the weight.'],
    ['weightKg', { weightKg: 'heavy' }, 'Enter a valid weight.'],
    ['weightKg', { weightKg: '10000' }, 'Use a weight from 0 to 9999.9.'],
    ['weightKg', { weightKg: '6.95' }, 'Use at most 1 decimal place.'],
    ['abilities', { abilities: ' , ' }, 'Enter at least one ability.'],
    ['localizedName', { localizedName: 'a'.repeat(101) }, 'Use at most 100 characters.'],
    ['region', { region: 'a'.repeat(101) }, 'Use at most 100 characters.'],
    ['internalTags', { internalTags: 'a'.repeat(31) }, 'Each tag can have at most 30 characters.'],
  ])('rejects an invalid %s', (field, change, message) => {
    const result = localPokemonFormSchema.safeParse({ ...valid, ...change })
    expect(result.error?.issues[0].path[0]).toBe(field)
    expect(result.error?.issues[0].message).toBe(message)
  })

  it('rejects more than 10 abilities and more than 20 tags', () => {
    const list = (count: number) => Array.from({ length: count }, (_, i) => `item${i}`).join(',')
    const result = localPokemonFormSchema.safeParse({
      ...valid,
      abilities: list(11),
      internalTags: list(21),
    })
    expect(result.error?.issues.map((issue) => issue.message)).toEqual([
      'Use at most 10 abilities.',
      'Use at most 20 tags.',
    ])
  })
})

describe('localPokemonUpdateSchema', () => {
  it('rejects repeated abilities and tags that differ only in case', () => {
    const result = localPokemonUpdateSchema.safeParse({
      name: 'bulbasaur',
      spriteUrl: null,
      category: null,
      weightKg: 6.9,
      abilities: ['overgrow', 'overgrow'],
      localizedName: null,
      region: null,
      internalTags: ['Starter', 'starter'],
    })
    expect(result.error?.issues.map((issue) => issue.message)).toEqual([
      'Abilities must not repeat.',
      'Tags must not repeat.',
    ])
  })
})

describe('registerSchema', () => {
  const valid = {
    name: 'Misty',
    email: 'misty@pokedex.dev',
    password: 'water123',
    confirmPassword: 'water123',
  }

  it('accepts a valid account', () => {
    expect(registerSchema.safeParse(valid).success).toBe(true)
  })

  it('requires 8 to 72 characters in the password', () => {
    const short = registerSchema.safeParse({ ...valid, password: 'abc', confirmPassword: 'abc' })
    expect(short.error?.issues[0].message).toBe('Use at least 8 characters.')
    const long = 'a'.repeat(73)
    const tooLong = registerSchema.safeParse({ ...valid, password: long, confirmPassword: long })
    expect(tooLong.error?.issues[0].message).toBe('Use at most 72 characters.')
  })

  it('rejects a blank name and an invalid email', () => {
    const result = registerSchema.safeParse({ ...valid, name: ' ', email: 'misty' })
    expect(result.error?.issues.map((issue) => issue.path)).toEqual([['name'], ['email']])
  })
})
