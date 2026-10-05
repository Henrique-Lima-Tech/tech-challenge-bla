import { QueryClientProvider } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { Toaster } from 'sonner'
import { AuthProvider } from '../features/auth/AuthProvider'
import { createQueryClient } from './queryClient'

export function AppProviders({ children }: Readonly<{ children: ReactNode }>) {
  const [queryClient] = useState(createQueryClient)

  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        {children}
        <Toaster richColors position="top-right" />
      </AuthProvider>
    </QueryClientProvider>
  )
}
