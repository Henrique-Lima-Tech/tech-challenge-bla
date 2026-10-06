# Pokémon API

A **Spring Boot REST API** that consumes the public [PokéAPI](https://pokeapi.co/docs/v2), replicates Pokémon into a local relational database with proprietary fields and protects that data with authentication, plus a **React frontend** that consumes the API.

Built with **Clean Architecture** and **TDD**.

---

## Contents

1. [Components](#components)
2. [User stories](#user-stories)
3. [Folder structure](#folder-structure)
4. [How the parts fit together](#how-the-parts-fit-together)
5. [Running with Docker](#running-with-docker)
6. [Running without Docker](#running-without-docker)
7. [Running the tests](#running-the-tests)
8. [Tech stack](#tech-stack)
9. [Documentation](#documentation)

---

## Components

| Component | What it is | Details |
|---|---|---|
| **Backend** | Java 25 + Spring Boot 4 REST API in 4 Maven modules (`domain`, `application`, `infrastructure`, `web`). PokéAPI client with cache, H2 database with Flyway, JWT authentication. | [`backend/README.md`](backend/README.md) |
| **Frontend** | React + TypeScript SPA (Vite) with list and details screens ("Add to My Pokémon"), My Pokémon (list, edit and delete), login and protected routes. | [`frontend/README.md`](frontend/README.md) |
| **Docs** | Requirements, API contract, architecture, decisions and one spec per stage. | [`docs/`](docs) |

---

## User stories

| User story | Backend endpoint | Frontend |
|---|---|---|
| **US01** Pokémon enumeration: paginated list with sprite, category, weight and abilities (cached) | `GET /api/v1/pokemon` | Pokémon list |
| **US02** Detailed view: image, base stats, description and evolution chain | `GET /api/v1/pokemon/{idOrName}` | Pokémon details |
| **US03** Data synchronization: local copy with localized name, region and internal tags | `POST /api/v1/local/pokemon` | "Add to My Pokémon" button on the details page |
| **US04** Local data modification, with 404/400 validation | `PUT /api/v1/local/pokemon/{id}` | Edit form |
| CRUD of local Pokémon: list, get by id and delete (create and update are US03 and US04); each user has their own (D-31) | `GET /api/v1/local/pokemon`, `GET` and `DELETE /api/v1/local/pokemon/{id}` | My Pokémon list, edit and delete |
| Users and authentication: registration, JWT login, public and protected routes | `/api/v1/auth/register`, `/api/v1/auth/login` | Login and sign-up |

The full API reference (every route, field, status code and validation rule) is in [`backend/README.md`](backend/README.md#api-reference) and [`docs/api-contract.md`](docs/api-contract.md).

---

## Folder structure

This project lives in the `pokemon-api/` folder of the repository; the [repository README](../README.md) also describes the second project, `task-management/`.

```
pokemon-api/
├── backend/              Spring Boot API (see backend/README.md)
├── frontend/             React SPA (see frontend/README.md)
├── docs/                 Requirements, API contract, architecture, decisions, specs
├── docker-compose.yml    Starts the backend (with its database volume) and the frontend
└── README.md             This file
```

The rules and commands used by the AI agent that implemented each stage are at the repository root: `CLAUDE.md` and `.claude/`.

---

## How the parts fit together

```
Browser ──► Frontend (Vite dev server or Nginx) ──/api──► Backend :8080 ──► PokéAPI (cached)
                                                              │
                                                              └──► H2 database (file, Docker volume)
```

- The frontend calls the API on its **own origin** under `/api`. The Vite dev server (development) or Nginx (Docker) forwards those requests to the backend, so no CORS setup is needed.
- The catalog (US01, US02) always comes from the PokéAPI. Local data (US03, US04) lives in the H2 database.

---

## Running with Docker

Requirement: Docker with Compose. Nothing else (no Java or Node) is needed.

From the `pokemon-api/` folder:

```bash
docker compose up --build
```

| Service | URL | What it does |
|---|---|---|
| `frontend` | **http://localhost:3000** | Nginx serves the React build and forwards `/api` to the backend |
| `backend` | http://localhost:8080 | The REST API (also reachable directly, for example with curl) |

- Open **http://localhost:3000** in the browser: the frontend talks to the backend through Nginx, on the same origin.
- The H2 database lives in the `h2-data` volume, so data survives container re-creation.
- The JWT signing key comes from `backend/vault/JWT_SECRET` (a demo key), mounted read-only. See [Security](backend/README.md#security) before using it outside a local demo.

To start only one part: `docker compose up --build backend` (or `frontend`, which also starts the backend it depends on).

### Stopping

```bash
docker compose down        # stop both containers, keep the data
docker compose down -v     # stop both containers and delete the database volume
```

---

## Running without Docker

| Part | Requirement | Commands | URL |
|---|---|---|---|
| Backend | Java 25 | `cd backend && ./mvnw -q -DskipTests package && java -jar web/target/web-0.0.1-SNAPSHOT.jar` | http://localhost:8080 |
| Frontend | Node 20.19+ | `cd frontend && cp .env.example .env.development.local`, set `VITE_USE_MOCKS=false`, then `npm install && npm run dev` | http://localhost:5173 |

- Run the backend jar from `backend/`: the database file and the JWT key are resolved from the working directory.
- With `VITE_USE_MOCKS=true` (the default in `.env.example`), the frontend runs against a built-in mock and the backend is not needed.

More options and the configuration reference are in each component's README.

---

## Running the tests

| Part | Command | What runs |
|---|---|---|
| Backend | `cd backend && ./mvnw verify` | Unit tests (domain, use cases), Spring test slices and integration tests. The PokéAPI is never called: tests use recorded responses. |
| Frontend | `cd frontend && npm test` | Vitest + Testing Library, with the API mocked by MSW |

---

## Tech stack

| Part | Stack |
|---|---|
| Backend | Java 25, Spring Boot 4.1.1, Maven, H2 + Flyway, Spring `RestClient` + Caffeine cache, Spring Security OAuth2 Resource Server (JWT), MapStruct, Lombok, JUnit 5, Mockito, AssertJ |
| Frontend | React 19, TypeScript, Vite, React Router 7, TanStack Query 5, React Hook Form + Zod, CSS Modules, Vitest, Testing Library, MSW |
| Delivery | Docker (multi-stage images for both parts), Docker Compose, Nginx |

---

## Documentation

| Document | Content |
|---|---|
| [`backend/README.md`](backend/README.md) | Backend setup, configuration, API reference, architecture, database, security, troubleshooting |
| [`frontend/README.md`](frontend/README.md) | Frontend setup, scripts, environment variables, structure |
| [`docs/challenge/requirements.md`](docs/challenge/requirements.md) | Challenge requirements with IDs |
| [`docs/api-contract.md`](docs/api-contract.md) | Routes, JSON bodies, validation rules |
| [`docs/architecture.md`](docs/architecture.md) | Backend modules, packages, conventions, logging, reference patterns |
| [`docs/decisions.md`](docs/decisions.md) | Technical decisions and their reasons |
| [`docs/challenge/pokeapi.md`](docs/challenge/pokeapi.md) | The PokéAPI endpoints and fields used |
| [`docs/specs/`](docs/specs) | One spec per stage |
| [`frontend/docs/`](frontend/docs/README.md) | Frontend specs and design system |
