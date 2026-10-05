export const env = {
  /** Empty in the Docker image: nginx serves the app and forwards /api on the same origin. */
  apiUrl: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
  useMocks: import.meta.env.VITE_USE_MOCKS === 'true',
}
