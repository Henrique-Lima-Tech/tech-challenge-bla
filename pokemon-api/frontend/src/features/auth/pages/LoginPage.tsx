import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useLocation } from 'react-router'
import { isApiError } from '../../../shared/api/apiError'
import { Button } from '../../../shared/components/Button'
import { TextField } from '../../../shared/components/Field'
import { applyFieldErrors } from '../../../shared/forms/applyFieldErrors'
import { readFrom } from '../../../shared/navigationState'
import { useDocumentTitle } from '../../../shared/useDocumentTitle'
import { loginSchema, type LoginFormValues } from '../schemas'
import { useAuth } from '../useAuth'
import styles from './AuthPage.module.css'

const FIELDS = ['email', 'password'] as const
const AUTH_PAGES = new Set(['/login', '/register'])

function redirectTarget(state: unknown): string {
  const from = readFrom(state)
  return from && !AUTH_PAGES.has(from.split('?')[0]) ? from : '/pokemon'
}

function readEmail(state: unknown): string {
  if (typeof state !== 'object' || state === null || !('email' in state)) return ''
  return typeof state.email === 'string' ? state.email : ''
}

export function LoginPage() {
  useDocumentTitle('Sign in')
  const { login, status } = useAuth()
  const location = useLocation()
  const [formError, setFormError] = useState<string | null>(null)
  const initialEmail = readEmail(location.state)
  const {
    register,
    handleSubmit,
    setError,
    setFocus,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    mode: 'onSubmit',
    defaultValues: { email: initialEmail, password: '' },
  })

  useEffect(() => {
    setFocus(initialEmail ? 'password' : 'email')
  }, [initialEmail, setFocus])

  if (status === 'authenticated') return <Navigate to={redirectTarget(location.state)} replace />

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await login(values)
    } catch (error) {
      if (!isApiError(error)) return setFormError('Something went wrong. Please try again.')
      const unmatched = applyFieldErrors(error.fieldErrors, FIELDS, setError)
      if (error.fieldErrors.length === 0 || unmatched.length > 0) setFormError(error.message)
    }
  })

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <h1 className={styles.title}>Sign in</h1>
        <form className={styles.form} onSubmit={(event) => void onSubmit(event)} noValidate>
          <TextField
            label="Email"
            type="email"
            autoComplete="email"
            error={errors.email?.message}
            {...register('email')}
          />
          <TextField
            label="Password"
            type="password"
            autoComplete="current-password"
            error={errors.password?.message}
            {...register('password')}
          />
          {formError && (
            <p className={styles.alert} role="alert">
              {formError}
            </p>
          )}
          <Button type="submit" variant="primary" loading={isSubmitting} className={styles.submit}>
            Sign in
          </Button>
        </form>
        <p className={styles.footer}>
          Don’t have an account? <Link to="/register">Sign up</Link>
        </p>
      </div>
    </div>
  )
}
