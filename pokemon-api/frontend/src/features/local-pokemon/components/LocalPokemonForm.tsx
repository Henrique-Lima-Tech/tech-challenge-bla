import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { notify } from '../../../shared/notify'
import { isApiError } from '../../../shared/api/apiError'
import { Chip } from '../../../shared/components/Badge'
import { Button, ButtonLink } from '../../../shared/components/Button'
import { TextField } from '../../../shared/components/Field'
import { applyFieldErrors } from '../../../shared/forms/applyFieldErrors'
import {
  localPokemonFormSchema,
  MAX_ABILITIES,
  MAX_TAGS,
  parseList,
  type LocalPokemonFormOutput,
  type LocalPokemonFormValues,
} from '../schemas'
import type { LocalPokemon, LocalPokemonUpdate } from '../types'
import styles from './LocalPokemonForm.module.css'

const FIELDS = [
  'name',
  'spriteUrl',
  'category',
  'weightKg',
  'abilities',
  'localizedName',
  'region',
  'internalTags',
] as const

/**
 * Every input is text: lists are joined with commas and a missing optional value becomes an
 * empty string. `localPokemonFormSchema` turns the inputs back into a `LocalPokemonUpdate`.
 */
function toFormValues(pokemon: LocalPokemon): LocalPokemonFormValues {
  return {
    name: pokemon.name,
    spriteUrl: pokemon.spriteUrl ?? '',
    category: pokemon.category ?? '',
    weightKg: String(pokemon.weightKg),
    abilities: pokemon.abilities.join(', '),
    localizedName: pokemon.localizedName ?? '',
    region: pokemon.region ?? '',
    internalTags: pokemon.internalTags.join(', '),
  }
}

type LocalPokemonFormProps = {
  pokemon: LocalPokemon
  onSubmit: (input: LocalPokemonUpdate) => Promise<void>
}

/**
 * Edits every field of a local copy except its identifiers. The page loads the Pokémon and
 * handles the 404; this form validates, submits and shows the back end's field errors.
 */
export function LocalPokemonForm({ pokemon, onSubmit }: Readonly<LocalPokemonFormProps>) {
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors, isDirty, isSubmitting },
  } = useForm<LocalPokemonFormValues, unknown, LocalPokemonFormOutput>({
    resolver: zodResolver(localPokemonFormSchema),
    mode: 'onTouched',
    defaultValues: toFormValues(pokemon),
  })

  // The counters and the tags preview use the same normalization as the submitted value.
  const abilityCount = parseList(useWatch({ control, name: 'abilities' })).length
  const previewTags = parseList(useWatch({ control, name: 'internalTags' }))

  const submit = handleSubmit(async (values) => {
    try {
      await onSubmit(values)
    } catch (error) {
      if (!isApiError(error)) {
        notify.error('Could not save.')
        return
      }
      const unmatched = applyFieldErrors(error.fieldErrors, FIELDS, setError)
      // No field errors, or one with no matching input: the message still needs to show.
      if (error.fieldErrors.length === 0 || unmatched.length > 0) notify.error(error.message)
    }
  })

  return (
    <form className={styles.form} onSubmit={(event) => void submit(event)} noValidate>
      <fieldset className={styles.group}>
        <legend className={styles.legend}>Pokémon data</legend>
        <div className={styles.columns}>
          <TextField label="Name" error={errors.name?.message} {...register('name')} />
          <TextField
            label="Category"
            placeholder="e.g. Seed Pokémon"
            error={errors.category?.message}
            {...register('category')}
          />
          <TextField
            label="Weight (kg)"
            inputMode="decimal"
            error={errors.weightKg?.message}
            {...register('weightKg')}
          />
          <TextField
            label="Sprite URL"
            type="url"
            placeholder="https://..."
            error={errors.spriteUrl?.message}
            {...register('spriteUrl')}
          />
        </div>
        <TextField
          label="Abilities (comma separated)"
          placeholder="e.g. overgrow, chlorophyll"
          error={errors.abilities?.message}
          aside={`${abilityCount}/${MAX_ABILITIES}`}
          {...register('abilities')}
        />
      </fieldset>

      <fieldset className={styles.group}>
        <legend className={styles.legend}>Custom fields</legend>
        <div className={styles.columns}>
          <TextField
            label="Localized name"
            placeholder="e.g. Bulbizarre"
            error={errors.localizedName?.message}
            {...register('localizedName')}
          />
          <TextField
            label="Region"
            placeholder="e.g. Kanto"
            error={errors.region?.message}
            {...register('region')}
          />
        </div>
        <div className={styles.tagsField}>
          <TextField
            label="Tags (comma separated)"
            placeholder="e.g. starter, grass"
            error={errors.internalTags?.message}
            aside={`${previewTags.length}/${MAX_TAGS}`}
            {...register('internalTags')}
          />
          {previewTags.length > 0 && (
            <div className={styles.preview}>
              <span className={styles.previewLabel}>Preview:</span>
              <ul className={styles.previewTags} aria-label="Tags preview">
                {previewTags.map((tag) => (
                  <li key={tag}>
                    <Chip>{tag}</Chip>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </fieldset>

      <div className={styles.actions}>
        <ButtonLink to="/my-pokemon">Cancel</ButtonLink>
        <Button type="submit" variant="primary" loading={isSubmitting} disabled={!isDirty}>
          Save changes
        </Button>
      </div>
    </form>
  )
}
