import { Link, useLocation } from 'react-router'
import { Chip } from '../../../shared/components/Badge'
import { Button, ButtonLink } from '../../../shared/components/Button'
import { PokemonImage } from '../../../shared/components/PokemonImage'
import { capitalize, formatPokemonNumber } from '../../../shared/format'
import { fromState } from '../../../shared/navigationState'
import type { LocalPokemon } from '../types'
import styles from './LocalPokemonList.module.css'

type ListProps = {
  items: LocalPokemon[]
  onDelete: (pokemon: LocalPokemon) => void
}

const EMPTY = '—'

function PokemonName({ pokemon }: Readonly<{ pokemon: LocalPokemon }>) {
  const location = useLocation()
  return (
    <Link to={`/pokemon/${pokemon.pokeApiId}`} state={fromState(location)} className={styles.name}>
      <PokemonImage src={pokemon.spriteUrl} alt="" size={40} lazy className={styles.image} />
      <span className={styles.number}>{formatPokemonNumber(pokemon.pokeApiId)}</span>{' '}
      <span>{capitalize(pokemon.name)}</span>
    </Link>
  )
}

function Tags({ tags }: Readonly<{ tags: string[] }>) {
  if (tags.length === 0) return <span>{EMPTY}</span>
  return (
    <ul className={styles.tags}>
      {tags.map((tag) => (
        <li key={tag}>
          <Chip>{tag}</Chip>
        </li>
      ))}
    </ul>
  )
}

function Actions({
  pokemon,
  onDelete,
}: Readonly<{
  pokemon: LocalPokemon
  onDelete: ListProps['onDelete']
}>) {
  const name = capitalize(pokemon.name)
  return (
    <div className={styles.actions}>
      <ButtonLink to={`/my-pokemon/${pokemon.id}/edit`} size="sm" aria-label={`Edit ${name}`}>
        Edit
      </ButtonLink>
      <Button
        variant="ghost"
        size="sm"
        onClick={() => onDelete(pokemon)}
        aria-label={`Delete ${name}`}
      >
        Delete
      </Button>
    </div>
  )
}

export function LocalPokemonTable({ items, onDelete }: Readonly<ListProps>) {
  return (
    <div className={styles.tableWrapper}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th scope="col">Name</th>
            <th scope="col">Localized name</th>
            <th scope="col">Region</th>
            <th scope="col">Tags</th>
            <th scope="col">
              <span className="visually-hidden">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {items.map((pokemon) => (
            <tr key={pokemon.id}>
              <td>
                <PokemonName pokemon={pokemon} />
              </td>
              <td>{pokemon.localizedName ?? EMPTY}</td>
              <td>{pokemon.region ?? EMPTY}</td>
              <td>
                <Tags tags={pokemon.internalTags} />
              </td>
              <td>
                <Actions pokemon={pokemon} onDelete={onDelete} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function LocalPokemonCards({ items, onDelete }: Readonly<ListProps>) {
  return (
    <ul className={styles.cards}>
      {items.map((pokemon) => (
        <li key={pokemon.id} className={styles.card}>
          <PokemonName pokemon={pokemon} />
          <p className={styles.cardMeta}>
            {pokemon.localizedName ?? EMPTY} · {pokemon.region ?? EMPTY}
          </p>
          <Tags tags={pokemon.internalTags} />
          <Actions pokemon={pokemon} onDelete={onDelete} />
        </li>
      ))}
    </ul>
  )
}
