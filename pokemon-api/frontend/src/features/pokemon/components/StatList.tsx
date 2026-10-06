import { useId } from 'react'
import { cx } from '../../../shared/cx'
import type { PokemonStat } from '../types'
import styles from './StatList.module.css'

const STAT_LABELS: Record<string, string> = {
  hp: 'HP',
  attack: 'Attack',
  defense: 'Defense',
  'special-attack': 'Sp. Attack',
  'special-defense': 'Sp. Defense',
  speed: 'Speed',
}

const MAX_BASE_STAT = 255

function statLevel(value: number) {
  if (value >= 90) return styles.high
  if (value >= 50) return styles.mid
  return styles.low
}

function StatBar({ stat }: Readonly<{ stat: PokemonStat }>) {
  const labelId = useId()
  const label = STAT_LABELS[stat.name] ?? stat.name

  return (
    <div className={styles.row}>
      <dt id={labelId} className={styles.label}>
        {label}
      </dt>
      <dd className={styles.value}>{stat.baseStat}</dd>
      <dd className={styles.barCell}>
        <div
          role="meter"
          aria-labelledby={labelId}
          aria-valuemin={0}
          aria-valuemax={MAX_BASE_STAT}
          aria-valuenow={stat.baseStat}
          className={styles.track}
        >
          <div
            className={cx(styles.bar, statLevel(stat.baseStat))}
            style={{ width: `${Math.min(stat.baseStat / MAX_BASE_STAT, 1) * 100}%` }}
          />
        </div>
      </dd>
    </div>
  )
}

export function StatList({ stats }: Readonly<{ stats: PokemonStat[] }>) {
  const total = stats.reduce((sum, stat) => sum + stat.baseStat, 0)
  return (
    <dl className={styles.list}>
      {stats.map((stat) => (
        <StatBar key={stat.name} stat={stat} />
      ))}
      <div className={cx(styles.row, styles.total)}>
        <dt className={styles.label}>Total</dt>
        <dd className={styles.value}>{total}</dd>
      </div>
    </dl>
  )
}
