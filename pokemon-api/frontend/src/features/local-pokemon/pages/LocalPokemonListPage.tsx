import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { notify } from '../../../shared/notify'
import { isApiError } from '../../../shared/api/apiError'
import { ButtonLink } from '../../../shared/components/Button'
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog'
import { Pagination } from '../../../shared/components/Pagination'
import { Loading, Skeleton } from '../../../shared/components/Skeleton'
import { EmptyState, ErrorState } from '../../../shared/components/StateMessage'
import { cx } from '../../../shared/cx'
import { capitalize, formatInteger } from '../../../shared/format'
import { readPageParam, withPageParam } from '../../../shared/pageParam'
import { useDocumentTitle } from '../../../shared/useDocumentTitle'
import { useMediaQuery } from '../../../shared/useMediaQuery'
import { LocalPokemonCards, LocalPokemonTable } from '../components/LocalPokemonList'
import { useDeleteLocalPokemon, useLocalPokemonList } from '../queries'
import type { LocalPokemon } from '../types'
import styles from './LocalPokemonListPage.module.css'

const PAGE_SIZE = 20

export function LocalPokemonListPage() {
  useDocumentTitle('My Pokémon')
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const [searchParams, setSearchParams] = useSearchParams()
  const page = readPageParam(searchParams)
  const { data, isPending, isError, error, refetch, isPlaceholderData } = useLocalPokemonList({
    page: page - 1,
    size: PAGE_SIZE,
  })

  const [toDelete, setToDelete] = useState<LocalPokemon | null>(null)
  const deletion = useDeleteLocalPokemon()

  function confirmDelete() {
    if (!toDelete) return
    const deletedName = capitalize(toDelete.name)
    deletion.mutate(toDelete.id, {
      onSuccess: () => notify.success(`${deletedName} deleted`),
      onError: (deleteError) => {
        if (isApiError(deleteError) && deleteError.status === 404) {
          notify.info('This Pokémon had already been deleted')
        } else {
          notify.error(isApiError(deleteError) ? deleteError.message : 'Could not delete.')
        }
      },
      onSettled: () => setToDelete(null),
    })
  }

  function renderContent() {
    if (isPending) {
      return (
        <Loading>
          <div className={styles.skeleton}>
            {Array.from({ length: 3 }, (_, index) => (
              <Skeleton key={index} height="4rem" />
            ))}
          </div>
        </Loading>
      )
    }
    if (isError) return <ErrorState error={error} onRetry={() => void refetch()} />
    if (data.content.length === 0) {
      if (page > 1) {
        return (
          <EmptyState
            title="No Pokémon on this page"
            action={<ButtonLink to="/my-pokemon">Back to page 1</ButtonLink>}
          />
        )
      }
      return (
        <EmptyState
          title="No Pokémon saved yet"
          action={<ButtonLink to="/pokemon">View all Pokémon</ButtonLink>}
        >
          <p>
            Open a Pokémon and click <strong>Add to My Pokémon</strong>.
          </p>
        </EmptyState>
      )
    }

    const List = isDesktop ? LocalPokemonTable : LocalPokemonCards
    return (
      <>
        <div
          className={cx(isPlaceholderData && styles.stale)}
          aria-busy={isPlaceholderData || undefined}
        >
          <List items={data.content} onDelete={setToDelete} />
        </div>
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
        <h1 className={styles.title}>My Pokémon</h1>
        <p className={styles.subtitle}>
          {data ? `${formatInteger(data.totalElements)} Pokémon saved.` : 'Your saved Pokémon.'}
        </p>
      </div>

      {renderContent()}

      <ConfirmDialog
        open={toDelete !== null}
        title={`Delete ${toDelete ? capitalize(toDelete.name) : ''}?`}
        confirmLabel="Delete"
        pending={deletion.isPending}
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
      >
        <p>
          The local record and its custom fields will be deleted. The PokéAPI data is not affected.
        </p>
      </ConfirmDialog>
    </>
  )
}
