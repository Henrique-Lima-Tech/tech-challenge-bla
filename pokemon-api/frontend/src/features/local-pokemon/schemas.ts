import { z } from 'zod'

export const MAX_ABILITIES = 10
export const MAX_TAGS = 20

function normalizeList(items: string[]): string[] {
  const cleaned = items.map((item) => item.trim().toLowerCase()).filter((item) => item !== '')
  return [...new Set(cleaned)]
}

export function parseList(text: string): string[] {
  return normalizeList(text.split(','))
}

function hasNoDuplicates(items: string[]): boolean {
  return new Set(items.map((item) => item.toLowerCase())).size === items.length
}

function hasAtMostOneDecimal(value: number): boolean {
  return Math.abs(value * 10 - Math.round(value * 10)) < 1e-9
}

const maxLength = (max: number) => `Use at most ${max} characters.`

export const localPokemonUpdateSchema = z.object({
  name: z.string().trim().min(1, 'Enter the name.').max(50, maxLength(50)),
  spriteUrl: z
    .url({ protocol: /^https?$/, error: 'Enter a valid http or https URL.' })
    .max(500, maxLength(500))
    .nullable(),
  category: z.string().trim().max(50, maxLength(50)).nullable(),
  weightKg: z
    .number('Enter a valid weight.')
    .min(0, 'Use a weight from 0 to 9999.9.')
    .max(9999.9, 'Use a weight from 0 to 9999.9.')
    .refine(hasAtMostOneDecimal, 'Use at most 1 decimal place.'),
  abilities: z
    .array(
      z
        .string()
        .trim()
        .min(1, 'An ability cannot be blank.')
        .max(50, 'Each ability can have at most 50 characters.'),
    )
    .min(1, 'Enter at least one ability.')
    .max(MAX_ABILITIES, `Use at most ${MAX_ABILITIES} abilities.`)
    .refine(hasNoDuplicates, 'Abilities must not repeat.'),
  localizedName: z.string().trim().max(100, maxLength(100)).nullable(),
  region: z.string().trim().max(100, maxLength(100)).nullable(),
  internalTags: z
    .array(
      z
        .string()
        .trim()
        .min(1, 'A tag cannot be blank.')
        .max(30, 'Each tag can have at most 30 characters.'),
    )
    .max(MAX_TAGS, `Use at most ${MAX_TAGS} tags.`)
    .refine(hasNoDuplicates, 'Tags must not repeat.'),
})

const emptyToNull = (text: string) => (text.trim() === '' ? null : text.trim())
const fields = localPokemonUpdateSchema.shape

export const localPokemonFormSchema = z.object({
  name: z.string().pipe(fields.name),
  spriteUrl: z.string().transform(emptyToNull).pipe(fields.spriteUrl),
  category: z.string().transform(emptyToNull).pipe(fields.category),
  weightKg: z.string().trim().min(1, 'Enter the weight.').transform(Number).pipe(fields.weightKg),
  abilities: z.string().transform(parseList).pipe(fields.abilities),
  localizedName: z.string().transform(emptyToNull).pipe(fields.localizedName),
  region: z.string().transform(emptyToNull).pipe(fields.region),
  internalTags: z.string().transform(parseList).pipe(fields.internalTags),
})

export type LocalPokemonFormValues = z.input<typeof localPokemonFormSchema>
export type LocalPokemonFormOutput = z.output<typeof localPokemonFormSchema>
