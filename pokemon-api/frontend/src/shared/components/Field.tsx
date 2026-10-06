import { useId, type ComponentProps, type ReactNode } from 'react'
import { cx } from '../cx'
import styles from './Field.module.css'

type FieldProps = {
  label: string
  error?: string
  hint?: ReactNode
  aside?: ReactNode
  hideLabel?: boolean
  className?: string
}

type ControlProps = {
  id: string
  className: string
  'aria-invalid'?: true
  'aria-describedby'?: string
}

function FieldShell({
  id: givenId,
  label,
  error,
  hint,
  aside,
  hideLabel,
  className,
  children,
}: FieldProps & { id?: string; children: (control: ControlProps) => ReactNode }) {
  const autoId = useId()
  const id = givenId ?? autoId
  const hintId = hint ? `${id}-hint` : undefined
  const errorId = error ? `${id}-error` : undefined
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined

  return (
    <div className={cx(styles.field, className)}>
      <label htmlFor={id} className={cx(styles.label, hideLabel && 'visually-hidden')}>
        {label}
      </label>
      {children({
        id,
        className: styles.control,
        'aria-invalid': error ? true : undefined,
        'aria-describedby': describedBy,
      })}
      {(Boolean(hint) || Boolean(aside)) && (
        <div className={styles.meta}>
          {hint && (
            <p id={hintId} className={styles.hint}>
              {hint}
            </p>
          )}
          {aside && <span className={styles.aside}>{aside}</span>}
        </div>
      )}
      {error && (
        <p id={errorId} className={styles.error}>
          {error}
        </p>
      )}
    </div>
  )
}

export function TextField({
  label,
  error,
  hint,
  aside,
  hideLabel,
  className,
  id,
  ...inputProps
}: FieldProps & Omit<ComponentProps<'input'>, 'className'>) {
  return (
    <FieldShell {...{ id, label, error, hint, aside, hideLabel, className }}>
      {(control) => <input {...inputProps} {...control} />}
    </FieldShell>
  )
}
