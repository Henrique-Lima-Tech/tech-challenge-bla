import { useState } from 'react'
import { cx } from '../cx'
import styles from './PokemonImage.module.css'

type PokemonImageProps = {
  src: string | null
  alt: string
  size: number
  lazy?: boolean
  className?: string
}

export function PokemonImage({
  src,
  alt,
  size,
  lazy = false,
  className,
}: Readonly<PokemonImageProps>) {
  const [failedSrc, setFailedSrc] = useState<string | null>(null)

  if (!src || failedSrc === src) {
    return (
      <span
        className={cx(styles.placeholder, className)}
        role={alt ? 'img' : undefined}
        aria-label={alt || undefined}
        aria-hidden={alt ? undefined : true}
      >
        ?
      </span>
    )
  }

  return (
    <img
      className={cx(styles.image, className)}
      src={src}
      alt={alt}
      width={size}
      height={size}
      loading={lazy ? 'lazy' : undefined}
      decoding="async"
      onError={() => setFailedSrc(src)}
    />
  )
}
