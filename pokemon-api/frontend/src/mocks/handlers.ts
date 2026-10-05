import { delay, http, HttpResponse } from 'msw'
import { z } from 'zod'
import { registerRequestSchema } from '../features/auth/schemas'
import { localPokemonUpdateSchema } from '../features/local-pokemon/schemas'
import type { FieldError, Page, ProblemDetail } from '../shared/api/types'
import { env } from '../shared/config/env'
import { findMockPokemon, MOCK_POKEMON, toDetails, toSummary } from './data'
import {
  db,
  pokemonToLocal,
  toPublicLocal,
  toPublicUser,
  type MockLocalPokemon,
  type MockUser,
} from './db'

const api = (path: string) => `${env.apiUrl}/api/v1${path}`

const TITLES: Record<number, string> = {
  400: 'Bad Request',
  401: 'Unauthorized',
  404: 'Not Found',
  409: 'Conflict',
}

const VALIDATION_FAILED = 'Validation failed'

function problem(status: number, detail: string, errors?: FieldError[]) {
  const body: ProblemDetail = {
    type: 'about:blank',
    title: TITLES[status],
    status,
    detail,
    ...(errors && { errors }),
  }
  return HttpResponse.json(body, { status })
}

function zodErrors(error: z.ZodError): FieldError[] {
  return error.issues.map((issue) => ({
    field: issue.path
      .map((part) => (typeof part === 'number' ? `[${part}]` : `.${String(part)}`))
      .join('')
      .slice(1),
    message: issue.message,
  }))
}

/**
 * Mock tokens are `mock-token-<email>`, not JWTs.
 */
function userFromRequest(request: Request): MockUser | undefined {
  const token = request.headers.get('Authorization')?.replace('Bearer ', '')
  const email = token?.startsWith('mock-token-') ? token.slice('mock-token-'.length) : null
  return db.users.find((u) => u.email === email)
}

function authorize(request: Request): MockUser | Response {
  return userFromRequest(request) ?? problem(401, 'Authentication required.')
}

function readPaging(url: URL): { page: number; size: number } | Response {
  const page = Number(url.searchParams.get('page') ?? 0)
  const size = Number(url.searchParams.get('size') ?? 20)
  const errors: FieldError[] = []
  if (!Number.isInteger(page) || page < 0) {
    errors.push({ field: 'page', message: 'must be greater than or equal to 0' })
  }
  if (!Number.isInteger(size) || size < 1 || size > 100) {
    errors.push({ field: 'size', message: 'must be between 1 and 100' })
  }
  return errors.length ? problem(400, VALIDATION_FAILED, errors) : { page, size }
}

function paginate<T>(items: T[], page: number, size: number): Page<T> {
  return {
    content: items.slice(page * size, page * size + size),
    page,
    size,
    totalElements: items.length,
    totalPages: Math.ceil(items.length / size),
  }
}

async function readJson(request: Request): Promise<unknown> {
  try {
    return await request.json()
  } catch {
    return undefined
  }
}

/**
 * Another user's Pokémon answers 404, like a missing one (D-31).
 */
function findLocal(idParam: string, user: MockUser): MockLocalPokemon | Response {
  const id = Number(idParam)
  if (!Number.isInteger(id) || id <= 0) return problem(400, VALIDATION_FAILED)
  const pokemon = db.localPokemon.find((p) => p.id === id && p.userId === user.id)
  return pokemon ?? problem(404, 'Local Pokémon not found.')
}

const createSchema = z.object({
  pokemon: z.string().trim().min(1, 'must not be blank').max(50, 'size must be at most 50'),
  localizedName: localPokemonUpdateSchema.shape.localizedName.optional(),
  region: localPokemonUpdateSchema.shape.region.optional(),
  internalTags: localPokemonUpdateSchema.shape.internalTags.optional(),
})

/**
 * Stand-in for the back end in the tests and in `npm run dev` with `VITE_USE_MOCKS=true`, following
 * docs/api-contract.md.
 */
export const handlers = [
  http.get(api('/pokemon'), async ({ request }) => {
    await delay()
    const paging = readPaging(new URL(request.url))
    if (paging instanceof Response) return paging
    return HttpResponse.json(paginate(MOCK_POKEMON.map(toSummary), paging.page, paging.size))
  }),

  http.get(api('/pokemon/:idOrName'), async ({ params }) => {
    await delay()
    const idOrName = String(params.idOrName)
    if (idOrName.trim() === '') return problem(400, VALIDATION_FAILED)
    const pokemon = findMockPokemon(idOrName)
    if (!pokemon) return problem(404, 'Pokémon not found.')
    return HttpResponse.json(toDetails(pokemon))
  }),

  http.get(api('/local/pokemon'), async ({ request }) => {
    await delay()
    const auth = authorize(request)
    if (auth instanceof Response) return auth
    const paging = readPaging(new URL(request.url))
    if (paging instanceof Response) return paging
    const own = db.localPokemon.filter((p) => p.userId === auth.id).sort((a, b) => a.id - b.id)
    return HttpResponse.json(paginate(own.map(toPublicLocal), paging.page, paging.size))
  }),

  http.get(api('/local/pokemon/:id'), async ({ request, params }) => {
    await delay()
    const auth = authorize(request)
    if (auth instanceof Response) return auth
    const pokemon = findLocal(String(params.id), auth)
    return pokemon instanceof Response ? pokemon : HttpResponse.json(toPublicLocal(pokemon))
  }),

  http.post(api('/local/pokemon'), async ({ request }) => {
    await delay()
    const auth = authorize(request)
    if (auth instanceof Response) return auth
    const body = await readJson(request)
    if (body === undefined) return problem(400, VALIDATION_FAILED)
    const parsed = createSchema.safeParse(body)
    if (!parsed.success) return problem(400, VALIDATION_FAILED, zodErrors(parsed.error))

    const { pokemon: idOrName, ...custom } = parsed.data
    const created = pokemonToLocal(idOrName, db.nextLocalId)
    if (!created) return problem(404, 'Pokémon not found.')
    if (db.localPokemon.some((p) => p.userId === auth.id && p.pokeApiId === created.pokeApiId)) {
      return problem(409, 'This Pokémon is already in My Pokémon.')
    }
    const saved: MockLocalPokemon = { ...created, ...custom, userId: auth.id }
    db.nextLocalId++
    db.localPokemon.push(saved)
    return HttpResponse.json(toPublicLocal(saved), {
      status: 201,
      headers: { Location: `/api/v1/local/pokemon/${saved.id}` },
    })
  }),

  http.put(api('/local/pokemon/:id'), async ({ request, params }) => {
    await delay()
    const auth = authorize(request)
    if (auth instanceof Response) return auth
    const existing = findLocal(String(params.id), auth)
    if (existing instanceof Response) return existing

    const body = await readJson(request)
    if (typeof body !== 'object' || body === null) return problem(400, VALIDATION_FAILED)
    if ('id' in body || 'pokeApiId' in body) return problem(400, VALIDATION_FAILED)
    const parsed = localPokemonUpdateSchema.safeParse(body)
    if (!parsed.success) return problem(400, VALIDATION_FAILED, zodErrors(parsed.error))

    const updated: MockLocalPokemon = { ...existing, ...parsed.data }
    db.localPokemon[db.localPokemon.indexOf(existing)] = updated
    return HttpResponse.json(toPublicLocal(updated))
  }),

  http.delete(api('/local/pokemon/:id'), async ({ request, params }) => {
    await delay()
    const auth = authorize(request)
    if (auth instanceof Response) return auth
    const existing = findLocal(String(params.id), auth)
    if (existing instanceof Response) return existing
    db.localPokemon.splice(db.localPokemon.indexOf(existing), 1)
    return new HttpResponse(null, { status: 204 })
  }),

  http.post(api('/auth/login'), async ({ request }) => {
    await delay()
    const body = (await readJson(request)) as { email?: string; password?: string } | undefined
    if (!body?.email || !body.password) return problem(400, VALIDATION_FAILED)
    const user = db.users.find(
      (u) => u.email === body.email?.trim().toLowerCase() && u.password === body.password,
    )
    if (!user) return problem(401, 'Invalid email or password.')
    return HttpResponse.json({
      accessToken: `mock-token-${user.email}`,
      tokenType: 'Bearer',
      expiresIn: 3600,
      name: user.name,
    })
  }),

  http.post(api('/auth/register'), async ({ request }) => {
    await delay()
    const body = await readJson(request)
    if (body === undefined) return problem(400, VALIDATION_FAILED)
    const parsed = registerRequestSchema.safeParse(body)
    if (!parsed.success) return problem(400, VALIDATION_FAILED, zodErrors(parsed.error))

    const { name, email, password } = parsed.data
    if (db.users.some((u) => u.email.toLowerCase() === email.toLowerCase())) {
      return problem(409, 'This email is already registered.')
    }
    const user: MockUser = { id: db.nextUserId++, name, email: email.toLowerCase(), password }
    db.users.push(user)
    return HttpResponse.json(toPublicUser(user), { status: 201 })
  }),
]
