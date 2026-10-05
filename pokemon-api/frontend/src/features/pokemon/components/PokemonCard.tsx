import { Link, useLocation } from 'react-router'
import { Chip } from '../../../shared/components/Badge'
import { PokemonImage } from '../../../shared/components/PokemonImage'
import { Skeleton } from '../../../shared/components/Skeleton'
import { capitalize, formatKg, formatPokemonNumber } from '../../../shared/format'
import { fromState } from '../../../shared/navigationState'
import type { PokemonSummary } from '../types'
import styles from './PokemonCard.module.css'

export function PokemonCard({ pokemon }: Readonly<{ pokemon: PokemonSummary }>) {
  const location = useLocation()
  const name = capitalize(pokemon.name)

  return (
    <Link to={`/pokemon/${pokemon.id}`} state={fromState(location)} className={styles.card}>
      <PokemonImage src={pokemon.spriteUrl} alt={name} size={96} lazy className={styles.image} />
      <div className={styles.body}>
        <span className={styles.number}>{formatPokemonNumber(pokemon.id)}</span>
        <h2 className={styles.name}>{name}</h2>
        <p className={styles.meta}>
          {pokemon.category ?? 'Unknown category'}
          <span aria-hidden="true"> · </span>
          <span className="visually-hidden">, weight </span>
          {formatKg(pokemon.weightKg)}
        </p>
        {pokemon.abilities.length > 0 && (
          <ul className={styles.abilities} aria-label="Abilities">
            {pokemon.abilities.map((ability) => (
              <li key={ability}>
                <Chip>{ability}</Chip>
              </li>
            ))}
          </ul>
        )}
      </div>
    </Link>
  )
}

export function PokemonCardSkeleton() {
  return (
    <div className={styles.card}>
      <Skeleton circle className={styles.image} />
      <div className={styles.body}>
        <Skeleton width="3rem" />
        <Skeleton width="60%" height="1.25rem" />
        <Skeleton width="80%" />
        <Skeleton width="70%" height="1.5rem" />
      </div>
    </div>
  )
}
