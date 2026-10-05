import { useSearchParams } from 'react-router'
import { ButtonLink } from '../../../shared/components/Button'
import { Pagination } from '../../../shared/components/Pagination'
import { Loading } from '../../../shared/components/Skeleton'
import { EmptyState, ErrorState } from '../../../shared/components/StateMessage'
import { cx } from '../../../shared/cx'
import { formatInteger } from '../../../shared/format'
import { readPageParam, withPageParam } from '../../../shared/pageParam'
import { useDocumentTitle } from '../../../shared/useDocumentTitle'
import { PokemonCard, PokemonCardSkeleton } from '../components/PokemonCard'
import { usePokemonList } from '../queries'
import styles from './PokemonListPage.module.css'

const PAGE_SIZE = 20
const SKELETON_COUNT = 8

export function PokemonListPage() {
  useDocumentTitle('Pokémon')
  const [searchParams, setSearchParams] = useSearchParams()
  const page = readPageParam(searchParams)
  const { data, isPending, isError, error, refetch, isPlaceholderData } = usePokemonList(
    page - 1,
    PAGE_SIZE,
  )

  function renderContent() {
    if (isPending) {
      return (
        <Loading label="Loading Pokémon...">
          <ul className={styles.grid}>
            {Array.from({ length: SKELETON_COUNT }, (_, index) => (
              <li key={index}>
                <PokemonCardSkeleton />
              </li>
            ))}
          </ul>
        </Loading>
      )
    }
    if (isError) return <ErrorState error={error} onRetry={() => void refetch()} />
    if (data.content.length === 0) {
      return (
        <EmptyState
          title="No Pokémon on this page"
          action={<ButtonLink to="/pokemon">Back to page 1</ButtonLink>}
        >
          <p>Page {page} does not exist.</p>
        </EmptyState>
      )
    }
    return (
      <>
        <ul
          className={cx(styles.grid, isPlaceholderData && styles.stale)}
          aria-busy={isPlaceholderData || undefined}
        >
          {data.content.map((pokemon) => (
            <li key={pokemon.id}>
              <PokemonCard pokemon={pokemon} />
            </li>
          ))}
        </ul>
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          onPageChange={(next) => setSearchParams(withPageParam(searchParams, next))}
        />
      </>
    )
  }

  return (
    <>
      <div className={styles.header}>
        <h1 className={styles.title}>Pokémon</h1>
        <p className={styles.subtitle}>
          {data
            ? `${formatInteger(data.totalElements)} Pokémon from the PokéAPI.`
            : 'Pokémon from the PokéAPI.'}
        </p>
      </div>

      {renderContent()}
    </>
  )
}
