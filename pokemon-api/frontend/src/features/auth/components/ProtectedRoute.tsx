import { Navigate, Outlet, useLocation } from 'react-router'
import { fromState } from '../../../shared/navigationState'
import { useAuth } from '../useAuth'

export function ProtectedRoute() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'anonymous') return <Navigate to="/login" replace state={fromState(location)} />
  return <Outlet />
}
