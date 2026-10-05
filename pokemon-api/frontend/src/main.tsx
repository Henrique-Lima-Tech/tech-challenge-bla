import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AppProviders } from './app/AppProviders'
import App from './App'
import { env } from './shared/config/env'
import './index.css'

async function enableMocks() {
  if (!import.meta.env.DEV || !env.useMocks) return
  const { worker } = await import('./mocks/browser')
  await worker.start({ onUnhandledRequest: 'bypass', quiet: true })
}

const root = document.getElementById('root')
if (!root) throw new Error('Missing #root element in index.html')

try {
  await enableMocks()
} catch (error) {
  console.error('Could not start the mock (MSW).', error)
}

createRoot(root).render(
  <StrictMode>
    <AppProviders>
      <App />
    </AppProviders>
  </StrictMode>,
)
