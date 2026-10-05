import { Button } from './Button'
import styles from './Pagination.module.css'

type PaginationProps = {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, onPageChange }: Readonly<PaginationProps>) {
  if (totalPages <= 1) return null
  const isFirst = page <= 0
  const isLast = page >= totalPages - 1

  return (
    <nav className={styles.pagination} aria-label="Pagination">
      <Button onClick={() => onPageChange(page - 1)} disabled={isFirst}>
        <span aria-hidden="true">‹</span> Previous
      </Button>
      <p className={styles.status}>
        Page {page + 1} of {totalPages}
      </p>
      <Button onClick={() => onPageChange(page + 1)} disabled={isLast}>
        Next <span aria-hidden="true">›</span>
      </Button>
    </nav>
  )
}
