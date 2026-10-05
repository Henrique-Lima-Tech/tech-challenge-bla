import { useEffect } from 'react'

export function useDocumentTitle(title: string | null) {
  useEffect(() => {
    if (title !== null) document.title = `${title} · Pokédex`
  }, [title])
}
