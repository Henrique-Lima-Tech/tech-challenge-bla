import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StatList } from './StatList'

describe('StatList', () => {
  it('labels each stat, keeps unknown names as they come and shows the total', () => {
    render(
      <StatList
        stats={[
          { name: 'hp', baseStat: 45 },
          { name: 'accuracy', baseStat: 100 },
        ]}
      />,
    )

    expect(screen.getByRole('meter', { name: 'HP' })).toHaveAttribute('aria-valuenow', '45')
    expect(screen.getByRole('meter', { name: 'accuracy' })).toHaveAttribute('aria-valuenow', '100')
    expect(screen.getByText('145')).toBeInTheDocument()
  })
})
