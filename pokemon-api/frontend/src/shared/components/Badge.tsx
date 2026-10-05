import type { ReactNode } from 'react'
import styles from './Badge.module.css'

export function Chip({ children }: Readonly<{ children: ReactNode }>) {
  return <span className={styles.chip}>{children}</span>
}
