# Backend: Pokémon API

RESTful API in **Java 25 + Spring Boot 4** that consumes the [PokéAPI](https://pokeapi.co/docs/v2), replicates Pokémon into a local database with proprietary fields (localized name, region, internal tags) and protects that data with JWT authentication. Built with **Clean Architecture** and **TDD**.

All commands run from `backend/`.

## Running

The whole stack (backend + frontend) runs with `docker compose up --build` from `pokemon-api/` (see the [project README](../README.md)). Without Docker, with **Java 25**:

```bash
./mvnw -q -DskipTests package
java -jar web/target/web-0.0.1-SNAPSHOT.jar      # http://localhost:8080
```

- Run the jar from `backend/`: the H2 file (`./data/`) and the JWT key (`./vault/JWT_SECRET`) are resolved from the working directory. Flyway creates the schema on startup.
- The JWT key comes from the environment variable `JWT_SECRET` or, if not set, from the file `vault/JWT_SECRET` (a demo key, at least 32 bytes). From an IDE or `./mvnw spring-boot:run`, set `JWT_SECRET` in the environment.
- Other settings (database URL, PokéAPI URL, timeouts, cache) are in `web/src/main/resources/application.yaml`.

## Tests

```bash
./mvnw verify
```

| Layer | What is tested | How |
|---|---|---|
| `domain` | Invariants of the models | Plain JUnit, no Spring |
| `application` | Use cases | JUnit + Mockito on the output ports |
| `infrastructure` | PokéAPI client, adapter, mappers, cache; repositories and migrations | `MockRestServiceServer` with **recorded PokéAPI responses** (no test calls the real API); `@DataJpaTest` on H2 in memory |
| `web` | Controllers, validation, error format, security (401 without, with invalid and with expired tokens) | `@WebMvcTest` + `MockMvc`, plus integration tests |

## API

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

- **Pagination:** `page` starts at 0; `size` defaults to 20 and accepts 1 to 100. Response: `{ "content": [...], "page", "size", "totalElements", "totalPages" }`. A page past the end returns an empty `content`.
- **404:** the record does not exist or belongs to another user (on sync, the PokéAPI does not know the Pokémon). **409:** the email is already registered, or the user already synced that Pokémon. **502:** the PokéAPI is unreachable or too slow.

### Validation

Sync (`POST`) takes `pokemon` (id or name, required, ≤ 50 characters) plus the proprietary fields. Update (`PUT`) replaces every field except `id` and `pokeApiId`.

| Field | Rule |
|---|---|
| `name` | Required, ≤ 50 characters |
| `spriteUrl` | Optional, valid `http`/`https` URL, ≤ 500 characters |
| `category` | Optional, ≤ 50 characters |
| `weightKg` | Required, 0 to 9999.9, at most 1 decimal place |
| `abilities` | Required, 1 to 10 items, each ≤ 50 characters, no duplicates |
| `localizedName`, `region` | Optional, ≤ 100 characters |
| `internalTags` | Optional, ≤ 20 tags, each ≤ 30 characters, no duplicates (case-insensitive) |

Malformed JSON, an empty body, a non-numeric or ≤ 0 `{id}`, and `id`/`pokeApiId` in the PUT body all answer 400. Text fields are trimmed before validation.

### Error format

Every error is a `ProblemDetail` (RFC 9457), without stack traces. Validation errors list the failing fields:

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

## Architecture

```
web ──► application ──► domain
 │           ▲
 └──► infrastructure
```

| Module | Responsibility |
|---|---|
| `domain` | Models and domain exceptions. **Plain Java.** |
| `application` | Use cases: input ports, output ports (PokéAPI, repositories, password hashing, tokens) and services. **Plain Java.** |
| `infrastructure` | Adapters for the output ports: JPA persistence, PokéAPI client + cache, BCrypt, JWT, Flyway migrations. JPA entities and PokéAPI models never leave this module. |
| `web` | REST controllers, DTOs, MapStruct mappers, security configuration, global error handler. |

The dependency rule is enforced by the build: `domain` and `application` have no framework on their classpath, so a Spring or JPA import does not compile.

Key decisions:

- **Catalog vs local data.** US01/US02 read from the PokéAPI; US03/US04 work on the local database. The sync fetches the Pokémon from the PokéAPI and stores a copy with the proprietary fields.
- **Cache.** Every PokéAPI response is cached (Caffeine, 2,000 entries, 24 h), as the PokéAPI Fair Use Policy asks.
- **Parallel calls for US01.** The PokéAPI list returns only names, so a page of 20 needs 41 calls. They run in parallel on virtual threads, with a global limit of 10 concurrent calls.
- **Local Pokémon per user.** Each user manages only their own copies. A unique constraint on `(user_id, poke_api_id)` turns a duplicate sync into 409, even under concurrency.

## Database

H2 in file mode. Flyway owns the schema; Hibernate only validates it.

| Table | Columns |
|---|---|
| `users` | `id`, `name`, `email` (unique), `password_hash` |
| `local_pokemon` | `id`, `user_id`, `poke_api_id` (unique per user), `name`, `sprite_url`, `category`, `weight_kg`, `localized_name`, `region` |
| `local_pokemon_abilities`, `local_pokemon_internal_tags` | The lists of each local Pokémon |

To start from an empty database, stop the application and delete `data/` (with Docker: `docker compose down -v`).

## Security

- Passwords are stored only as BCrypt hashes (8 to 72 characters) and are never returned or logged.
- JWT signed with HS256, valid for 1 hour. A missing, invalid or expired token on a protected route gets 401. A failed login always gets the same generic 401.
- **Demo key:** `vault/JWT_SECRET` is versioned so the demo runs without setup. Use your own key (`JWT_SECRET`) anywhere other than a local demo.
