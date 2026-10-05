import { z } from 'zod'

const email = z.email('Enter a valid email.')

export const loginSchema = z.object({
  email: z.string().trim().min(1, 'Enter your email.').pipe(email),
  password: z.string().min(1, 'Enter your password.'),
})

/**
 * The register rules of the contract, so most 400s are caught before the request.
 */
export const registerRequestSchema = z.object({
  name: z.string().trim().min(1, 'Enter your name.').max(100, 'Use at most 100 characters.'),
  email: z.string().trim().pipe(email.max(254, 'Use at most 254 characters.')),
  password: z.string().min(8, 'Use at least 8 characters.').max(72, 'Use at most 72 characters.'),
})

export const registerSchema = registerRequestSchema
  .extend({ confirmPassword: z.string() })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match.',
    path: ['confirmPassword'],
  })

export type LoginFormValues = z.infer<typeof loginSchema>
export type RegisterFormValues = z.infer<typeof registerSchema>
