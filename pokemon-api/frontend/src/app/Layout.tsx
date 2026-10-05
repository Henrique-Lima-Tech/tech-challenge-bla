import { Suspense, useEffect, useRef } from 'react'
import { Link, NavLink, Outlet, ScrollRestoration, useLocation } from 'react-router'
import { notify } from '../shared/notify'
import pokeballUrl from '../assets/pokeball.png'
import { useAuth } from '../features/auth/useAuth'
import { Button, ButtonLink } from '../shared/components/Button'
import { PageSkeleton } from '../shared/components/Skeleton'
import { cx } from '../shared/cx'
import { fromState } from '../shared/navigationState'
import styles from './Layout.module.css'

const AUTH_PAGES = new Set(['/login', '/register'])

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  cx(styles.navLink, isActive && styles.active)

/**
 * Sign in for visitors (hidden on the sign in and sign up pages); the name and Sign out for
 * logged-in users.
 */
function UserArea() {
  const { status, name, logout } = useAuth()
  const location = useLocation()

  if (status === 'anonymous') {
    if (AUTH_PAGES.has(location.pathname)) return null
    return (
      <ButtonLink to="/login" state={fromState(location)} variant="primary" size="sm">
        Sign in
      </ButtonLink>
    )
  }

  function handleLogout() {
    logout()
    notify.success('You have signed out')
  }

  return (
    <>
      {name && (
        <span className={styles.userName}>
          <span className="visually-hidden">Signed in as </span>
          {name}
        </span>
      )}
      <Button variant="secondary" size="sm" onClick={handleLogout}>
        Sign out
      </Button>
    </>
  )
}

/**
 * Moves the focus to `<main>` after a route change, so keyboard and screen reader users start at
 * the new page instead of the link they clicked. Nothing happens if the focus is already inside.
 */
function useFocusMainOnNavigation() {
  const mainRef = useRef<HTMLElement>(null)
  const { pathname } = useLocation()
  const previousPathname = useRef(pathname)

  useEffect(() => {
    if (previousPathname.current === pathname) return
    previousPathname.current = pathname
    const main = mainRef.current
    if (main && !main.contains(document.activeElement)) main.focus({ preventScroll: true })
  }, [pathname])

  return mainRef
}

export function Layout() {
  const { status } = useAuth()
  const mainRef = useFocusMainOnNavigation()

  return (
    <>
      <a href="#content" className={styles.skipLink}>
        Skip to content
      </a>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <Link to="/pokemon" className={styles.brand}>
            <img src={pokeballUrl} alt="" width={28} height={28} className={styles.logo} />
            Pokédex
          </Link>
          <nav className={styles.nav} aria-label="Main">
            <NavLink to="/pokemon" className={navLinkClass}>
              Pokémon
            </NavLink>
            {status === 'authenticated' && (
              <NavLink to="/my-pokemon" className={navLinkClass}>
                My Pokémon
              </NavLink>
            )}
          </nav>
          <div className={styles.user}>
            <UserArea />
          </div>
        </div>
      </header>
      <main id="content" ref={mainRef} className={styles.main} tabIndex={-1}>
        <Suspense fallback={<PageSkeleton />}>
          <Outlet />
        </Suspense>
      </main>
      <ScrollRestoration />
    </>
  )
}
