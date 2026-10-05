import { describe, expect, it } from 'vitest'
import { capitalize, formatInteger, formatKg, formatPokemonNumber } from './format'

describe('format', () => {
  it('pads the Pokémon number to 3 digits', () => {
    expect(formatPokemonNumber(1)).toBe('#001')
    expect(formatPokemonNumber(25)).toBe('#025')
    expect(formatPokemonNumber(1025)).toBe('#1025')
  })

  it('capitalizes the first letter', () => {
    expect(capitalize('bulbasaur')).toBe('Bulbasaur')
    expect(capitalize('')).toBe('')
  })

  it('formats numbers in en-US', () => {
    expect(formatKg(6.9)).toBe('6.9 kg')
    expect(formatKg(100)).toBe('100 kg')
    expect(formatInteger(1302)).toBe('1,302')
  })
})
