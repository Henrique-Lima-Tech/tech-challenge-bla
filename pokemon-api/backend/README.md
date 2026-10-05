# Backend: Pokémon API

The backend of the [Tech Challenge](../README.md): a RESTful API in **Java 25 + Spring Boot 4** that:

- consumes the public [PokéAPI](https://pokeapi.co/docs/v2);
- replicates Pokémon into a local relational database with proprietary fields (localized name, region, internal tags);
- protects that local data with user registration and JWT authentication: each user has their own local Pokémon.

Built with **Clean Architecture** (4 Maven modules, dependencies pointing inward) and **TDD**.

---

## Contents

1. [Features](#features)
2. [Module structure](#module-structure)
3. [Tech stack](#tech-stack)
4. [Environment setup](#environment-setup)
5. [Running the application](#running-the-application)
6. [Running the tests](#running-the-tests)
7. [API reference](#api-reference)
8. [Architecture](#architecture)
9. [Database](#database)
10. [Security](#security)
11. [Troubleshooting](#troubleshooting)
12. [Further documentation](#further-documentation)

---

## Features

| User story | What the API does | Endpoint |
|---|---|---|
| **US01** Pokémon enumeration | Paginated list with sprite, category, weight (kg) and abilities; PokéAPI responses are cached | `GET /api/v1/pokemon` |
| **US02** Detailed view | Image, base stats, narrative description and the full evolution chain (with branches and a sprite per stage) | `GET /api/v1/pokemon/{idOrName}` |
| **US03** Data synchronization | Copies a Pokémon from the PokéAPI into the local database, with proprietary fields: localized name, region, internal tags | `POST /api/v1/local/pokemon` |
| **US04** Local data modification | Updates a stored Pokémon with defensive validation: 404 for missing records, 400 for malformed payloads | `PUT /api/v1/local/pokemon/{id}` |
| CRUD of local Pokémon | List (paginated), get by id and delete the local copies, besides create (US03) and update (US04). Each user only sees and changes their own copies (D-31) | `GET`, `DELETE` `/api/v1/local/pokemon[/{id}]` |
| Users and authentication | Registration, login with JWT, public and protected routes | `/api/v1/auth/**` |

---

## Module structure

```
backend/
├── domain/              Enterprise rules: models, value objects, domain exceptions (plain Java)
├── application/         Use cases, input/output ports (plain Java)
├── infrastructure/      Adapters: JPA persistence, PokéAPI client, cache, security, Flyway migrations
├── web/                 Entry point: REST controllers, DTOs, security configuration, error handling
├── vault/               Simulated secret store (JWT signing key for the demo)
├── Dockerfile
└── mvnw                 Maven wrapper (no local Maven needed)
```

All commands in this file run from `backend/`, unless stated otherwise.

---

## Tech stack

| Concern | Choice |
|---|---|
| Language / framework | Java 25, Spring Boot 4.1.1, Maven (wrapper included) |
| Database | H2 in file mode (application), H2 in memory (tests) |
| Migrations | Flyway |
| PokéAPI client | Spring `RestClient`, virtual threads for parallel calls |
| Cache | Spring Cache + Caffeine (max 2,000 entries, 24 h) |
| Security | Spring Security OAuth2 Resource Server (JWT, HS256), BCrypt |
| Errors | `ProblemDetail` (RFC 9457) from one `@RestControllerAdvice` |
| Mapping / boilerplate | MapStruct, Lombok, records |
| Tests | JUnit 5, AssertJ, Mockito, Spring test slices, `MockRestServiceServer` with recorded PokéAPI responses |

---

## Environment setup

### Prerequisites

- **Java 25** (`java -version`). Maven is not needed: the project uses the wrapper `./mvnw`.
- **Docker** with Compose: only to run in a container.
- Internet access to `https://pokeapi.co` at runtime. The tests do **not** need it.

### Configuration

All settings live in `web/src/main/resources/application.yaml`. These can be overridden:

| Setting | How to set it | Default |
|---|---|---|
| JWT signing key (`security.jwt.secret`) | Environment variable `JWT_SECRET`, or the file `vault/JWT_SECRET` relative to the working directory | `vault/JWT_SECRET` (demo key). HS256, at least 32 bytes |
| Database URL | `DB_URL` | `jdbc:h2:file:./data/challenge` (relative to the working directory) |
| Database user / password | `DB_USERNAME` / `DB_PASSWORD` | `sa` / empty |
| JWT lifetime | `security.jwt.expiration` | `1h` |
| PokéAPI base URL | `pokeapi.base-url` | `https://pokeapi.co/api/v2` |
| Max concurrent PokéAPI calls | `pokeapi.max-concurrent-calls` | `10` |
| HTTP client timeouts | `spring.http.clients.connect-timeout` / `read-timeout` | `2s` / `5s` |

**The `vault/` folder:** `spring.config.import: optional:configtree:./vault/` turns every file in `./vault` into a property (file name = key, content = value), the same way secrets are mounted by Vault or Kubernetes. An environment variable with the same name takes precedence.

To use your own key:

```bash
openssl rand -base64 48 > vault/JWT_SECRET
# or
export JWT_SECRET="$(openssl rand -base64 48)"
```

---

## Running the application

The API listens on **http://localhost:8080**.

### Option 1: Docker Compose (recommended)

From the **`pokemon-api/` folder** (`docker-compose.yml` lives there and also starts the frontend):

```bash
docker compose up --build backend
```

Without the service name, `docker compose up --build` starts the backend and the frontend (http://localhost:3000); see the [root README](../README.md#running-with-docker).

- `Dockerfile` is a two-stage build: the 4 modules are compiled with the Maven wrapper on a JDK 25 image, and the jar runs on a JRE 25 image as a non-root user.
- The H2 database file lives in the `h2-data` volume, so data survives container re-creation.
- `vault/` is mounted read-only at `/app/vault` to provide the JWT key. The key is never copied into the image.

To stop the API: `docker compose down`. To also delete the data: `docker compose down -v`.

### Option 2: Docker without Compose

```bash
docker build -t tech-challenge-backend .
docker run -p 8080:8080 \
  -v h2-data:/app/data \
  -e JWT_SECRET="$(openssl rand -base64 48)" \
  tech-challenge-backend
```

### Option 3: Java

```bash
./mvnw -q -DskipTests package
java -jar web/target/web-0.0.1-SNAPSHOT.jar
```

- Run the jar **from `backend/`**: the H2 file (`./data/`) and the key (`./vault/JWT_SECRET`) are resolved from the working directory.
- With an IDE or `./mvnw spring-boot:run`, the working directory is `backend/web`, where `vault/` does not exist. In that case set `JWT_SECRET` in the environment.
- On startup, Flyway creates or updates the database schema automatically.

---

## Running the tests

```bash
./mvnw verify                      # full build: all modules, all tests
./mvnw -pl domain test             # one module
./mvnw -pl web -am test            # one module and the modules it depends on
```

| Layer | What is tested | How |
|---|---|---|
| `domain` | Invariants of the models and value objects | Plain JUnit, no Spring |
| `application` | Use cases | JUnit + Mockito mocks of the output ports |
| `infrastructure` / PokéAPI | Client, adapter, mappers, cache, parallel calls | `@RestClientTest` + `MockRestServiceServer` serving **recorded PokéAPI responses** (`infrastructure/src/test/resources/fixtures/pokeapi`). No test calls the real API. |
| `infrastructure` / persistence | Repositories, adapters, migrations, constraint translation | `@DataJpaTest` on H2 in memory |
| `web` | Controllers, validation, error format, security rules (401 without, with invalid and with expired tokens) | `@WebMvcTest` + `MockMvc`, plus integration tests |

---

## API reference

The full contract, with every field and validation rule, is in [`docs/api-contract.md`](../docs/api-contract.md).

### Endpoints

| Method | Route | Access | Success | Errors |
|---|---|---|---|---|
| GET | `/api/v1/pokemon?page={n}&size={m}` | Public | 200 | 400, 502 |
| GET | `/api/v1/pokemon/{idOrName}` | Public | 200 | 400, 404, 502 |
| POST | `/api/v1/auth/register` | Public | 201 | 400, 409 |
| POST | `/api/v1/auth/login` | Public | 200 | 400, 401 |
| GET | `/api/v1/local/pokemon?page={n}&size={m}` | **JWT** | 200 | 400, 401 |
| GET | `/api/v1/local/pokemon/{id}` | **JWT** | 200 | 400, 401, 404 |
| POST | `/api/v1/local/pokemon` | **JWT** | 201 + `Location` | 400, 401, 404, 409, 502 |
| PUT | `/api/v1/local/pokemon/{id}` | **JWT** | 200 | 400, 401, 404 |
| DELETE | `/api/v1/local/pokemon/{id}` | **JWT** | 204 | 400, 401, 404 |

- **Pagination:** `page` starts at 0 (default 0); `size` defaults to 20 and accepts 1 to 100. A page past the end returns an empty `content` with the real totals. The local list is sorted by `id`.
- **Response shape:** `{ "content": [...], "page", "size", "totalElements", "totalPages" }`.
- **Status codes:**
  - 404: the record does not exist or belongs to another user (or, on sync, the PokéAPI does not know the Pokémon);
  - 409: the email is already registered, or the user already synced that Pokémon;
  - 502: the PokéAPI is unreachable or too slow.

### Quick tour with curl

The login step uses [`jq`](https://jqlang.org/) to extract the token.

```bash
# Public catalog (US01, US02)
curl "http://localhost:8080/api/v1/pokemon?page=0&size=5"
curl "http://localhost:8080/api/v1/pokemon/pikachu"

# Register and log in
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Ash","email":"ash@example.com","password":"pikachu123"}'

TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ash@example.com","password":"pikachu123"}' | jq -r .accessToken)

# Sync a Pokémon into the local database with proprietary fields (US03)
curl -X POST http://localhost:8080/api/v1/local/pokemon \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"pokemon":"pikachu","localizedName":"Pikachu","region":"Kanto","internalTags":["starter"]}'

# Update it (US04): replaces every field except id and pokeApiId
curl -X PUT http://localhost:8080/api/v1/local/pokemon/1 \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"pikachu","category":"Mouse Pokémon","weightKg":6.0,"abilities":["static"],"region":"Kanto","internalTags":["mascot"]}'

# List and read the local copies
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/local/pokemon?page=0&size=20"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/local/pokemon/1

# Delete it (204); the same Pokémon can be synced again afterwards
curl -X DELETE -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/local/pokemon/1
```

### Validation

Sync (`POST`) accepts `pokemon` (id or name, required, ≤ 50 characters) plus the proprietary fields. Update (`PUT`) accepts every field except `id` and `pokeApiId`.

| Field | Rule |
|---|---|
| `name` | Required, ≤ 50 characters |
| `spriteUrl` | Optional, valid `http`/`https` URL, ≤ 500 characters |
| `category` | Optional, ≤ 50 characters |
| `weightKg` | Required, 0 to 9999.9, at most 1 decimal place |
| `abilities` | Required, 1 to 10 items, each ≤ 50 characters, no duplicates |
| `localizedName`, `region` | Optional, ≤ 100 characters |
| `internalTags` | Optional, ≤ 20 tags, each ≤ 30 characters, no duplicates (case-insensitive) |

Further defensive rules:

| Case | Response |
|---|---|
| Malformed JSON or empty body | 400 |
| Path `{id}` not numeric or ≤ 0 | 400 |
| `id` or `pokeApiId` sent in the PUT body | 400 (rejected, not silently ignored) |
| Text fields | Trimmed before validation |

### Error format

Every error is a `ProblemDetail` (RFC 9457). Validation errors add the failing fields:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/v1/local/pokemon/1",
  "errors": [ { "field": "weightKg", "message": "must be less than or equal to 9999.9" } ]
}
```

- Responses never contain stack traces.
- A failed login always returns the same generic 401, whether or not the email exists.

---

## Architecture

### Modules and dependency rule

```
web ──► application ──► domain
 │           ▲
 └──► infrastructure
```

| Module | Responsibility | Depends on |
|---|---|---|
| `domain` | Enterprise rules: models, value objects, domain exceptions. **Plain Java**: no Spring, JPA or Jackson. | nothing |
| `application` | Use cases. Input ports (use case interfaces), output ports (PokéAPI, repositories, password hashing, tokens), commands/results, services. **Plain Java.** | `domain` |
| `infrastructure` | Adapters implementing the output ports: JPA persistence, PokéAPI client + cache, BCrypt, JWT issuing, Flyway migrations. JPA entities and PokéAPI models never leave this module. | `application` |
| `web` | Entry point: REST controllers, request/response DTOs, MapStruct mappers, security configuration, global error handler, wiring of use cases as Spring beans. | `application`, `infrastructure` |

The dependency rule is **enforced by the build**:
- `domain` and `application` have no framework on their classpath, so a Spring or JPA import does not compile.
- Inside each module, packages are organized by feature (`pokemon`, `user`, `shared`).

### Request flow (example: US03 sync)

```
LocalPokemonController (web)
  └─► SyncPokemonUseCase (application, input port)
        └─► SyncPokemonService
              ├─► PokemonCatalogPort ──► PokeApiCatalogAdapter ──► PokeApiClient (cached) ──► PokéAPI
              └─► LocalPokemonRepositoryPort ──► LocalPokemonPersistenceAdapter ──► Spring Data JPA ──► H2
```

### Key design decisions

The full log, with the reason for each decision, is in [`docs/decisions.md`](../docs/decisions.md).

- **Catalog vs local data.**
  - US01/US02 always read from the PokéAPI.
  - US03/US04 work on the local database.
  - `POST /api/v1/local/pokemon` fetches the Pokémon from the PokéAPI and stores a local copy with the proprietary fields.
- **Cache.** Every PokéAPI response is cached (Caffeine, 2,000 entries, 24 h). One cache per resource, so the same Pokémon serves the list, the details and the sync. This also follows the PokéAPI Fair Use Policy (*"Locally cache resources whenever you request them"*).
- **Parallel calls for US01.**
  - The PokéAPI list endpoint returns only names. A page of 20 items therefore needs 41 calls (the list, plus one Pokémon and one species call per item).
  - They run in parallel on virtual threads.
  - A **global** limit of 10 concurrent calls protects the PokéAPI.
- **Concurrency-safe sync.** Duplicates are prevented by a unique constraint on `poke_api_id`, translated to 409. Two simultaneous syncs of the same Pokémon give one 201 and one 409.
- **Security without custom filters.** The Spring OAuth2 Resource Server validates the JWTs. Routes are either public or authenticated (no roles).
- **Local Pokémon per user (D-31).** Each authenticated user manages only their own local Pokémon; another user's id answers 404, and two users can sync the same Pokémon.

---

## Database

H2 in file mode. Flyway owns the schema; Hibernate only validates it (`ddl-auto: validate`).

| Migration | Tables |
|---|---|
| `V1__create_users_table.sql` | `users` (`id`, `name`, `email` unique, `password_hash`) |
| `V2__create_local_pokemon_tables.sql` | `local_pokemon` (`id`, `poke_api_id` unique, `name`, `sprite_url`, `category`, `weight_kg`, `localized_name`, `region`), `local_pokemon_abilities`, `local_pokemon_internal_tags` |

- Migrations live in `infrastructure/src/main/resources/db/migration`.
- Never edit an applied migration: add a new one.
- To start from an empty database locally, stop the application and delete `data/`. With Docker: `docker compose down -v`.

---

## Security

- Passwords are stored only as BCrypt hashes, and are never returned or logged. Length is limited to 8–72 characters, because BCrypt only uses the first 72 bytes.
- JWT signed with HS256. Lifetime: 1 hour. Requests to protected routes with a missing, invalid or expired token get 401.
- Emails, tokens and passwords are never written to the logs.
- No CORS configuration: the frontend reaches the API through a same-origin proxy (Vite in development, Nginx in Docker), so browsers on other origins cannot call it directly.
- ⚠️ **Demo key:** `vault/JWT_SECRET` is versioned so the demo runs without setup. Anyone who reads the repository can sign valid tokens with it, so **use your own key** (see [Configuration](#configuration)) in any environment that is not a local demo.

---

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| The first PokéAPI request after startup returns 502 | The first TLS connection to the PokéAPI can exceed the 5 s read timeout. Retry (later calls are fast), or raise `spring.http.clients.read-timeout`. |
| `docker compose up` fails with "port is already allocated" | Something else uses port 8080. Stop it, or change the mapping in `docker-compose.yml` (for example `"8081:8080"`). |
| The application fails at startup with an unresolved `JWT_SECRET` | The key was not found. Run from `backend/`, or set `JWT_SECRET` in the environment. |
| Flyway logs "H2 ... is newer than the version Flyway has been verified with" | Informational warning; it does not affect the application. |

---

## Further documentation

| Document | Content |
|---|---|
| [`../README.md`](../README.md) | The whole project: backend + frontend, running everything with Docker |
| [`../docs/challenge/requirements.md`](../docs/challenge/requirements.md) | Challenge requirements with IDs |
| [`../docs/api-contract.md`](../docs/api-contract.md) | Routes, JSON bodies, validation rules |
| [`../docs/architecture.md`](../docs/architecture.md) | Modules, packages, conventions, logging, reference patterns, Spring Boot 4 notes |
| [`../docs/decisions.md`](../docs/decisions.md) | Technical decisions and their reasons |
| [`../docs/challenge/pokeapi.md`](../docs/challenge/pokeapi.md) | The PokéAPI endpoints and fields used |
| [`../docs/specs/`](../docs/specs) | One spec per stage (rules, decisions, tasks) |
| [`../frontend/README.md`](../frontend/README.md) | Web client |
