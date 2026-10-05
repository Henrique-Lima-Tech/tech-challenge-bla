import { useDocumentTitle } from '../useDocumentTitle'
import { ButtonLink } from './Button'
import { StatusPage } from './StateMessage'

type NotFoundPageProps = {
  title?: string
  message?: string
}

export function NotFoundPage({
  title = 'Page not found',
  message = 'The page you are looking for does not exist.',
}: Readonly<NotFoundPageProps>) {
  useDocumentTitle(title)
  return (
    <StatusPage
      icon="?"
      title={title}
      action={
        <ButtonLink to="/pokemon" variant="primary">
          View all Pokémon
        </ButtonLink>
      }
    >
      <p>{message}</p>
    </StatusPage>
  )
}
