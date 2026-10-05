import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: { '/api': process.env.BACKEND_URL ?? 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    env: { VITE_API_URL: 'http://localhost:8080' },
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        'src/**/*.test.{ts,tsx}',
        'src/test/**',
        'src/mocks/**',
        'src/main.tsx',
        'src/vite-env.d.ts',
      ],
      reporter: ['text', 'html'],
    },
  },
})
