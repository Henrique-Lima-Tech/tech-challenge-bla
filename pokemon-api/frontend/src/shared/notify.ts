import { toast, type ExternalToast } from 'sonner'

export const notify = {
  success: (message: string) => toast.success(message, { duration: 3000 }),
  info: (message: string) => toast.info(message, { duration: 3000 }),
  error: (message: string, options: ExternalToast = {}) =>
    toast.error(message, { duration: options.action ? Infinity : 5000, ...options }),
}
