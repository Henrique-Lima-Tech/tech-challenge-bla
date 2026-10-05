import type { ComponentProps } from 'react'
import { Link } from 'react-router'
import { cx } from '../cx'
import styles from './Button.module.css'

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost'
type Size = 'md' | 'sm'

type ButtonProps = ComponentProps<'button'> & {
  variant?: Variant
  size?: Size
  loading?: boolean
}

export function Button({
  variant = 'secondary',
  size = 'md',
  loading = false,
  disabled = false,
  type = 'button',
  className,
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      className={cx(styles.button, styles[variant], styles[size], className)}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...props}
    >
      {loading && <span className={styles.spinner} aria-hidden="true" />}
      {children}
    </button>
  )
}

type ButtonLinkProps = ComponentProps<typeof Link> & { variant?: Variant; size?: Size }

export function ButtonLink({
  variant = 'secondary',
  size = 'md',
  className,
  ...props
}: ButtonLinkProps) {
  return <Link className={cx(styles.button, styles[variant], styles[size], className)} {...props} />
}
