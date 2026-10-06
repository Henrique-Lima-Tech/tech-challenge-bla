import type { ReactNode } from 'react'
import { isApiError } from '../api/apiError'
import { Button } from './Button'
import styles from './StateMessage.module.css'

type StateMessageProps = {
  icon: string
  title: string
  children?: ReactNode
  action?: ReactNode
  headingLevel?: 1 | 2
  role?: 'alert'
}

function StateMessage({
  icon,
  title,
  children,
  action,
  headingLevel = 2,
  role,
}: Readonly<StateMessageProps>) {
  const Heading = headingLevel === 1 ? 'h1' : 'h2'
  return (
    <div className={styles.state} role={role}>
      <span className={styles.icon} aria-hidden="true">
        {icon}
      </span>
      <Heading className={styles.title}>{title}</Heading>
      {children && <div className={styles.text}>{children}</div>}
      {action && <div className={styles.action}>{action}</div>}
    </div>
  )
}

export function EmptyState(props: Omit<StateMessageProps, 'icon' | 'role'> & { icon?: string }) {
  return <StateMessage icon="?" {...props} />
}

type ErrorStateProps = {
  error: unknown
  onRetry?: () => void
  headingLevel?: 1 | 2
}

export function ErrorState({ error, onRetry, headingLevel }: Readonly<ErrorStateProps>) {
  const offline = isApiError(error) && (error.status === 0 || error.status >= 500)
  const title = offline ? 'Could not connect to the server' : 'Could not load'
  const message = isApiError(error) ? error.message : 'Something went wrong.'

  return (
    <StateMessage
      icon="!"
      title={title}
      headingLevel={headingLevel}
      role="alert"
      action={onRetry && <Button onClick={onRetry}>Try again</Button>}
    >
      <p>{message}</p>
    </StateMessage>
  )
}

export function StatusPage(props: Readonly<Omit<StateMessageProps, 'headingLevel' | 'role'>>) {
  return (
    <div className={styles.page}>
      <StateMessage headingLevel={1} {...props} />
    </div>
  )
}
