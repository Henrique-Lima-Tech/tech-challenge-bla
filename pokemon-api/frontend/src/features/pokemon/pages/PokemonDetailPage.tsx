import { useId, type ReactNode } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { notify } from '../../../shared/notify'
import { isApiError } from '../../../shared/api/apiError'
import { Button } from '../../../shared/components/Button'
import { PokemonImage } from '../../../shared/components/PokemonImage'
import { Loading, Skeleton } from '../../../shared/components/Skeleton'
import { ErrorState } from '../../../shared/components/StateMessage'
import { NotFoundPage } from '../../../shared/components/StatusPages'
import { capitalize, formatPokemonNumber } from '../../../shared/format'
import { readFrom } from '../../../shared/navigationState'
import { useDocumentTitle } from '../../../shared/useDocumentTitle'
import { useAuth } from '../../auth/useAuth'
import { useCreateLocalPokemon } from '../../local-pokemon/queries'
import { EvolutionChain } from '../components/EvolutionChain'
import { StatList } from '../components/StatList'
import { usePokemonDetails } from '../queries'
import type { PokemonDetails } from '../types'
import styles from './PokemonDetailPage.module.css'

function Section({ title, children }: Readonly<{ title: string; children: ReactNode }>) {
  const id = useId()
  return (
    <section className={styles.section} aria-labelledby={id}>
      <h2 id={id} className={styles.sectionTitle}>
        {title}
      </h2>
      {children}
    </section>
  )
}

function SyncAction({ pokemon }: Readonly<{ pokemon: PokemonDetails }>) {
  const create = useCreateLocalPokemon()

  function handleSync() {
    const name = capitalize(pokemon.name)
    create.mutate(
      { pokemon: pokemon.name },
      {
        onSuccess: () => notify.success(`${name} saved to My Pokémon`),
        onError: (error) => {
          if (isApiError(error) && error.status === 409) {
            notify.info(`${name} is already in My Pokémon`)
          } else {
            notify.error(isApiError(error) ? error.message : 'Could not sync.')
          }
        },
      },
    )
  }

  return (
    <div className={styles.actions}>
      <Button variant="primary" onClick={handleSync} loading={create.isPending}>
        <span aria-hidden="true">+</span> Add to My Pokémon
      </Button>
      <p className={styles.actionHint}>
        Saves a copy you can edit in <Link to="/my-pokemon">My Pokémon</Link>.
      </p>
    </div>
  )
}

function PokemonDetailsView({
  pokemon,
  backTo,
}: Readonly<{ pokemon: PokemonDetails; backTo: string }>) {
  const { status } = useAuth()
  const name = capitalize(pokemon.name)

  return (
    <article className={styles.detail}>
      <Link to={backTo} className={styles.back}>
        <span aria-hidden="true">‹</span> Back to list
      </Link>

      <div className={styles.hero}>
        <div className={styles.imageBox}>
          <PokemonImage src={pokemon.imageUrl} alt={name} size={240} className={styles.image} />
        </div>
        <div className={styles.info}>
          <span className={styles.number}>{formatPokemonNumber(pokemon.id)}</span>
          <h1 className={styles.name}>{name}</h1>
          {status === 'authenticated' && <SyncAction pokemon={pokemon} />}
        </div>
      </div>

      <Section title="Description">
        <p className={styles.description}>{pokemon.description ?? 'No description.'}</p>
      </Section>

      <Section title="Stats">
        <StatList stats={pokemon.stats} />
      </Section>

      <Section title="Evolution chain">
        <EvolutionChain chain={pokemon.evolutionChain} currentName={pokemon.name} />
      </Section>
    </article>
  )
}

function DetailSkeleton() {
  return (
    <Loading label="Loading Pokémon...">
      <div className={styles.hero}>
        <Skeleton circle className={styles.image} />
        <div className={styles.info}>
          <Skeleton width="3rem" />
          <Skeleton width="50%" height="2.25rem" />
        </div>
      </div>
      <Skeleton height="6rem" />
    </Loading>
  )
}

export function PokemonDetailPage() {
  const { idOrName = '' } = useParams()
  const location = useLocation()
  const { data: pokemon, isPending, isError, error, refetch } = usePokemonDetails(idOrName)
  const title = pokemon ? capitalize(pokemon.name) : 'Pokémon'
  useDocumentTitle(isError && !pokemon ? null : title)

  if (isPending) return <DetailSkeleton />
  if (isError) {
    if (isApiError(error) && error.status === 404) {
      return (
        <NotFoundPage
          title="Pokémon not found"
          message={`There is no Pokémon with the name or number "${idOrName}".`}
        />
      )
    }
    return <ErrorState error={error} onRetry={() => void refetch()} headingLevel={1} />
  }

  return <PokemonDetailsView pokemon={pokemon} backTo={readFrom(location.state) ?? '/pokemon'} />
}
