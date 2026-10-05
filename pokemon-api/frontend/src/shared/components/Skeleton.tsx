import type { CSSProperties, ReactNode } from 'react'
import { cx } from '../cx'
import styles from './Skeleton.module.css'

type SkeletonProps = {
  width?: CSSProperties['width']
  height?: CSSProperties['height']
  circle?: boolean
  className?: string
}

export function Skeleton({
  width = '100%',
  height = '1em',
  circle = false,
  className,
}: Readonly<SkeletonProps>) {
  return (
    <span
      className={cx(styles.skeleton, circle && styles.circle, className)}
      style={{ width, height }}
      aria-hidden="true"
    />
  )
}

export function Loading({
  label = 'Loading...',
  children,
}: Readonly<{
  label?: string
  children: ReactNode
}>) {
  return (
    <div aria-busy="true">
      <span role="status" className="visually-hidden">
        {label}
      </span>
      {children}
    </div>
  )
}

export function PageSkeleton() {
  return (
    <Loading>
      <div className={styles.page}>
        <Skeleton width="40%" height="2.25rem" />
        <Skeleton height="12rem" />
      </div>
    </Loading>
  )
}
