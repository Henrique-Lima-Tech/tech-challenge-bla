import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AppProviders } from './app/AppProviders'
import App from './App'
import './index.css'

const root = document.getElementById('root')
if (!root) throw new Error('Missing #root element in index.html')

createRoot(root).render(
  <StrictMode>
    <AppProviders>
      <App />
    </AppProviders>
  </StrictMode>,
)
