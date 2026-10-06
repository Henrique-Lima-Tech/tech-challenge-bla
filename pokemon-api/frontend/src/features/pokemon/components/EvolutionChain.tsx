import { Link, useLocation } from 'react-router'
import { PokemonImage } from '../../../shared/components/PokemonImage'
import { cx } from '../../../shared/cx'
import { capitalize } from '../../../shared/format'
import type { EvolutionStage } from '../types'
import styles from './EvolutionChain.module.css'

function StagePokemon({
  stage,
  currentName,
}: Readonly<{ stage: EvolutionStage; currentName: string }>) {
  const location = useLocation()
  const backState: unknown = location.state
  const name = capitalize(stage.name)
  const content = (
    <>
      <PokemonImage src={stage.spriteUrl} alt="" size={96} lazy className={styles.image} />
      <span className={styles.name}>{name}</span>
    </>
  )

  if (stage.name === currentName) {
    return (
      <span className={cx(styles.pokemon, styles.current)} aria-current="page">
        {content}
        <span className={styles.currentLabel}>(current)</span>
      </span>
    )
  }
  return (
    <Link to={`/pokemon/${stage.name}`} state={backState} className={styles.pokemon}>
      {content}
    </Link>
  )
}

function Stage({ stage, currentName }: Readonly<{ stage: EvolutionStage; currentName: string }>) {
  return (
    <div className={styles.stage}>
      <StagePokemon stage={stage} currentName={currentName} />
      {stage.evolvesTo.length > 0 && (
        <ul className={styles.next}>
          {stage.evolvesTo.map((child) => (
            <li key={child.name} className={styles.branch}>
              <span className={styles.arrow}>
                <span className={styles.arrowGlyph} aria-hidden="true">
                  →
                </span>
                <span className="visually-hidden">evolves into</span>
              </span>
              <Stage stage={child} currentName={currentName} />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export function EvolutionChain({
  chain,
  currentName,
}: Readonly<{ chain: EvolutionStage; currentName: string }>) {
  if (chain.evolvesTo.length === 0) {
    return <p className={styles.none}>This Pokémon does not evolve.</p>
  }
  return (
    <div className={styles.chain}>
      <Stage stage={chain} currentName={currentName} />
    </div>
  )
}
