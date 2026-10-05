import { Link, useNavigate, useParams } from 'react-router'
import { notify } from '../../../shared/notify'
import { isApiError } from '../../../shared/api/apiError'
import { PokemonImage } from '../../../shared/components/PokemonImage'
import { Loading, Skeleton } from '../../../shared/components/Skeleton'
import { ErrorState } from '../../../shared/components/StateMessage'
import { NotFoundPage } from '../../../shared/components/StatusPages'
import { capitalize, formatPokemonNumber } from '../../../shared/format'
import { useDocumentTitle } from '../../../shared/useDocumentTitle'
import { LocalPokemonForm } from '../components/LocalPokemonForm'
import { useLocalPokemon, useUpdateLocalPokemon } from '../queries'
import type { LocalPokemon, LocalPokemonUpdate } from '../types'
import styles from './LocalPokemonEditPage.module.css'

function Summary({ pokemon }: Readonly<{ pokemon: LocalPokemon }>) {
  return (
    <div className={styles.base}>
      <PokemonImage src={pokemon.spriteUrl} alt="" size={64} className={styles.image} />
      <div>
        <p>
          <span className={styles.number}>{formatPokemonNumber(pokemon.pokeApiId)}</span>{' '}
          <strong>{capitalize(pokemon.name)}</strong>
        </p>
        <p className={styles.baseHint}>
          <Link to={`/pokemon/${pokemon.pokeApiId}`}>View details</Link>
        </p>
      </div>
    </div>
  )
}

function FormSkeleton() {
  return (
    <Loading>
      <div className={styles.skeleton}>
        <Skeleton height="4.5rem" />
        <Skeleton height="16rem" />
        <Skeleton height="10rem" />
      </div>
    </Loading>
  )
}

/**
 * Loads a local copy by its local id and hands it to `LocalPokemonForm`. An invalid id or a 404
 * shows the not-found page.
 */
export function LocalPokemonEditPage() {
  const { id: idParam } = useParams()
  const id = Number(idParam)
  const isValidId = Number.isInteger(id) && id > 0
  const navigate = useNavigate()
  // An invalid id never reaches the back end.
  const query = useLocalPokemon(id, isValidId)
  const update = useUpdateLocalPokemon(id)
  const pokemon = query.data
  const heading = pokemon ? `Edit ${capitalize(pokemon.name)}` : 'Edit'
  useDocumentTitle(query.isError && !pokemon ? null : heading)

  if (!isValidId || (query.isError && isApiError(query.error) && query.error.status === 404)) {
    return <NotFoundPage title="Pokémon not found" message="This Pokémon is not in My Pokémon." />
  }

  async function save(input: LocalPokemonUpdate) {
    // A failed update throws, and LocalPokemonForm shows its errors.
    await update.mutateAsync(input)
    notify.success('Changes saved')
    void navigate('/my-pokemon')
  }

  function renderContent() {
    if (query.isPending) return <FormSkeleton />
    if (query.isError) {
      return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
    }
    return (
      <>
        <Summary pokemon={query.data} />
        <LocalPokemonForm pokemon={query.data} onSubmit={save} />
      </>
    )
  }

  return (
    <div className={styles.page}>
      <Link to="/my-pokemon" className={styles.back}>
        <span aria-hidden="true">‹</span> My Pokémon
      </Link>
      <h1 className={styles.title}>{heading}</h1>

      {renderContent()}
    </div>
  )
}
