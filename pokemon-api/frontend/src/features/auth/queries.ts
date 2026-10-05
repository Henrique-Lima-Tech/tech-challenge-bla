import { useMutation } from '@tanstack/react-query'
import { authApi } from './api'
import type { RegisterRequest } from './types'

export function useRegister() {
  return useMutation({
    mutationFn: (data: RegisterRequest) => authApi.register(data),
  })
}
