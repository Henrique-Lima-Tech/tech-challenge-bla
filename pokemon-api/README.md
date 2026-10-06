# Pokémon API

A **Spring Boot REST API** that consumes the public [PokéAPI](https://pokeapi.co/docs/v2), replicates Pokémon into a local relational database with proprietary fields and protects that data with authentication, plus a **React frontend** that consumes the API. Built with **Clean Architecture** and **TDD**.

| Part | Details |
|---|---|
| [`backend/`](backend/README.md) | Java 25 + Spring Boot 4 in 4 Maven modules (`domain`, `application`, `infrastructure`, `web`): PokéAPI client with cache, H2 + Flyway, JWT. API reference, architecture and database in its README. |
| [`frontend/`](frontend/README.md) | React + TypeScript SPA (Vite). |

## User stories

| User story | Backend endpoint | Frontend |
|---|---|---|
| **US01** Paginated list with sprite, category, weight and abilities (cached) | `GET /api/v1/pokemon` | Pokémon list |
| **US02** Image, base stats, description and evolution chain | `GET /api/v1/pokemon/{idOrName}` | Pokémon details |
| **US03** Local copy with localized name, region and internal tags | `POST /api/v1/local/pokemon` | "Add to My Pokémon" on the details page |
| **US04** Local data modification, with 404/400 validation | `PUT /api/v1/local/pokemon/{id}` | Edit form |
| CRUD of local Pokémon (each user has their own) | `GET /api/v1/local/pokemon`, `GET` and `DELETE /api/v1/local/pokemon/{id}` | My Pokémon |
| Registration, JWT login, public and protected routes | `/api/v1/auth/register`, `/api/v1/auth/login` | Login and sign-up |

```
Browser ──► Frontend (Vite or Nginx) ──/api──► Backend :8080 ──► PokéAPI (cached)
                                                   └──► H2 database
```

The frontend calls the API on its own origin under `/api`; Vite (development) or Nginx (Docker) forwards it to the backend, so no CORS is needed.

## Running with Docker

Requirement: Docker with Compose. From `pokemon-api/`:

```bash
docker compose up --build
```

Open **http://localhost:3000** (the API is also on http://localhost:8080). The database lives in the `h2-data` volume; `docker compose down -v` deletes it. The JWT key is the demo key in `backend/vault/JWT_SECRET`.

## Running without Docker

| Part | Requirement | Commands | URL |
|---|---|---|---|
| Backend | Java 25 | `cd backend && ./mvnw -q -DskipTests package && java -jar web/target/web-0.0.1-SNAPSHOT.jar` | http://localhost:8080 |
| Frontend | Node 20.19+ | `cd frontend && cp .env.example .env.development.local && npm install && npm run dev` | http://localhost:5173 |

## Tests

| Part | Command | What runs |
|---|---|---|
| Backend | `cd backend && ./mvnw verify` | Unit, slice and integration tests. The PokéAPI is never called: tests use recorded responses. |
| Frontend | `cd frontend && npm test` | Vitest + Testing Library, with the API mocked by MSW |

## Tech stack

| Part | Stack |
|---|---|
| Backend | Java 25, Spring Boot 4.1.1, Maven, H2 + Flyway, `RestClient` + Caffeine, Spring Security OAuth2 Resource Server (JWT), MapStruct, Lombok, JUnit 5, Mockito, AssertJ |
| Frontend | React 19, TypeScript, Vite, React Router 7, TanStack Query 5, React Hook Form + Zod, CSS Modules, Vitest, Testing Library, MSW |
| Delivery | Docker (multi-stage images), Docker Compose, Nginx |
