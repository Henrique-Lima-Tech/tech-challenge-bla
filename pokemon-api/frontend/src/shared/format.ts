const decimal = new Intl.NumberFormat('en-US', { maximumFractionDigits: 1 })
const integer = new Intl.NumberFormat('en-US')

export function formatPokemonNumber(id: number): string {
  return `#${String(id).padStart(3, '0')}`
}

export function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1)
}

export function formatKg(kg: number): string {
  return `${decimal.format(kg)} kg`
}

export function formatInteger(value: number): string {
  return integer.format(value)
}
