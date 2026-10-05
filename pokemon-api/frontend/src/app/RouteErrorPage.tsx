import { isRouteErrorResponse, useRouteError } from 'react-router'
import { ButtonLink } from '../shared/components/Button'
import { StatusPage } from '../shared/components/StateMessage'
import { NotFoundPage } from '../shared/components/StatusPages'

export function RouteErrorPage() {
  const error = useRouteError()
  if (isRouteErrorResponse(error) && error.status === 404) return <NotFoundPage />

  return (
    <StatusPage
      icon="!"
      title="Something went wrong"
      action={<ButtonLink to="/pokemon">Back to home</ButtonLink>}
    >
      <p>An unexpected error occurred on this page. Try reloading.</p>
    </StatusPage>
  )
}
