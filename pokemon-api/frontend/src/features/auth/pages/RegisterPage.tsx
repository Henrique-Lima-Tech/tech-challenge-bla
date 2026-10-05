import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useNavigate } from 'react-router'
import { notify } from '../../../shared/notify'
import { isApiError } from '../../../shared/api/apiError'
import { Button } from '../../../shared/components/Button'
import { TextField } from '../../../shared/components/Field'
import { applyFieldErrors } from '../../../shared/forms/applyFieldErrors'
import { useDocumentTitle } from '../../../shared/useDocumentTitle'
import { useRegister } from '../queries'
import { registerSchema, type RegisterFormValues } from '../schemas'
import { useAuth } from '../useAuth'
import styles from './AuthPage.module.css'

const FIELDS = ['name', 'email', 'password', 'confirmPassword'] as const

export function RegisterPage() {
  useDocumentTitle('Sign up')
  const { status } = useAuth()
  const navigate = useNavigate()
  const registerUser = useRegister()
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    mode: 'onTouched',
    defaultValues: { name: '', email: '', password: '', confirmPassword: '' },
  })

  if (status === 'authenticated') return <Navigate to="/pokemon" replace />

  const onSubmit = handleSubmit(async ({ name, email, password }) => {
    setFormError(null)
    try {
      await registerUser.mutateAsync({ name, email, password })
      notify.success('Account created. Please sign in.')
      void navigate('/login', { state: { email } })
    } catch (error) {
      if (!isApiError(error)) return setFormError('Something went wrong. Please try again.')
      // A used email comes as a 409 without field errors; it belongs under the email input.
      if (error.status === 409 && error.fieldErrors.length === 0) {
        return setError('email', { type: 'server', message: error.message }, { shouldFocus: true })
      }
      const unmatched = applyFieldErrors(error.fieldErrors, FIELDS, setError)
      if (error.fieldErrors.length === 0 || unmatched.length > 0) setFormError(error.message)
    }
  })

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <h1 className={styles.title}>Create account</h1>
        <form className={styles.form} onSubmit={(event) => void onSubmit(event)} noValidate>
          <TextField
            label="Name"
            autoComplete="name"
            error={errors.name?.message}
            {...register('name')}
          />
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
            autoComplete="new-password"
            hint="8 to 72 characters."
            error={errors.password?.message}
            {...register('password')}
          />
          <TextField
            label="Confirm password"
            type="password"
            autoComplete="new-password"
            error={errors.confirmPassword?.message}
            {...register('confirmPassword')}
          />
          {formError && (
            <p className={styles.alert} role="alert">
              {formError}
            </p>
          )}
          <Button type="submit" variant="primary" loading={isSubmitting} className={styles.submit}>
            Create account
          </Button>
        </form>
        <p className={styles.footer}>
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
