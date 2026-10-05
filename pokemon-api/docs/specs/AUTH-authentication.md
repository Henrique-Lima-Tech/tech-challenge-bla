# AUTH: Registration, login and protected routes

- **Requirements:** REQ-API02, REQ-DB, REQ-DATA, REQ-CORE, REQ-T02, REQ-T03, REQ-TEST
- **Decisions used:** D-03, D-04, D-05, D-07, D-11, D-12, D-13, D-14, D-17, D-18, D-20, D-24, D-27
- **Status:** done (written 2026-10-03; Q1–Q4 answered with the recommendations)
- **Updated by D-32 (2026-10-05):** the login response also returns the user's `name`; `LoginUseCase` returns a `LoginResult` (the `AccessTokenResult` plus the name). The rest of this spec still holds.

Stage S3 of `docs/foundation-plan.md`. Reuses the US02/US01 reference patterns ("Reference patterns" in `docs/architecture.md`) and creates the first persistence and security patterns, which US03 to CRUD copy.

## Contract

Copied from `docs/api-contract.md`.

| Method | Route | Access | Requirement | Success | Errors |
|---|---|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | REQ-API02 | 201 | 400, 409 (email already used) |
| POST | `/api/v1/auth/login` | Public | REQ-API02 | 200 | 400, 401 |
| any | `/api/v1/local/**` | Authenticated | REQ-API02, D-11 | — | 401 |

**`POST /api/v1/auth/register`** request → 201 response

```json
{ "name": "Ash", "email": "ash@example.com", "password": "..." }
```
```json
{ "id": 1, "name": "Ash", "email": "ash@example.com" }
```

**`POST /api/v1/auth/login`** request → 200 response

```json
{ "email": "ash@example.com", "password": "..." }
```
```json
{ "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 3600, "name": "Ash" }
```

**Validation (D-27, contract "Auth")**

| Field | Rule |
|---|---|
| `name` (register) | Required, ≤ 100 chars |
| `email` | Required, valid email format, ≤ 254 chars |
| `password` (register) | Required, 8 to 72 chars (BCrypt only uses the first 72 bytes) |
| Login | `email` and `password` required; any credential failure is a 401 with a generic message |

General rules: malformed JSON or empty body → 400; text fields are trimmed before validation; any violation → 400 with `errors[]`.

**Errors** (D-17): `ProblemDetail` with `type`, `title`, `status`, `detail`, `instance`; validation errors add `errors[]` of `{field, message}`. 401 uses a generic message and never reveals whether the email exists. Never a stack trace.

## Rules

1. `POST /api/v1/auth/register` and `POST /api/v1/auth/login` are **public**; so is everything under `/api/v1/auth/**` (D-11; contract; stage notes).
2. `GET /api/v1/pokemon/**` stays public; `/api/v1/local/**` and every other route require a valid JWT. No roles (D-11; CLAUDE.md "Security").
3. JWT validation uses `spring-boot-starter-security-oauth2-resource-server` (`oauth2ResourceServer().jwt()`); no hand-written authentication filter (D-07).
4. A request to a protected route with no token, an invalid token (bad signature, malformed) or an expired token → **401** `ProblemDetail` with one generic `detail` for all three cases (CLAUDE.md "401 responses use a generic message"; stage notes).
5. A request to a protected route with a valid token passes security (stage notes).
6. Register stores the user with the password as a **BCrypt hash** only, and answers 201 with `id`, `name`, `email`. The password or its hash is never returned or logged (CLAUDE.md "Security"; contract).
7. Register with an email that is already stored → **409** `ProblemDetail` "Email already used" (contract).
8. Login with a known email and the right password → 200 with a signed JWT, `tokenType` `"Bearer"` and `expiresIn` in seconds (contract).
9. Login with an unknown email **or** a wrong password → **401** with the same generic `detail` (contract "Auth"; contract "Errors").
10. Validation as in the table above → **400** "Validation failed" + `errors[]` with explicit English messages (D-27; CLAUDE.md "Language").
11. Token issuing goes behind `TokenPort` (application `port.out`), implemented in `infrastructure/user/security/token`; password hashing behind `PasswordHasherPort`, implemented with BCrypt in `infrastructure/user/security/password` (stage notes; `architecture.md` "Role of each piece").
12. The JWT signing key comes from configuration (`security.jwt.secret: ${JWT_SECRET}`), resolved from the `JWT_SECRET` environment variable or the simulated vault file (A3); never written in a Java class (CLAUDE.md "Security"; stage notes; developer decision on the vault).
13. The `users` table is the first Flyway migration in `db/migration`, with only `id`, `name`, `email` (unique) and the password hash (D-14; REQ-DB; stage notes). No seed data (S7).
14. Persistence goes through `UserRepositoryPort`, implemented in `infrastructure/user/persistence` (entity, repository, mapper, adapter); persistence tests run on H2 in memory (REQ-DATA; D-13; D-24).
15. Logging as in "Logging" of `architecture.md`: `DEBUG` for a registration and a successful login with the user id; a failed login at `DEBUG` without email or password; never the password, hash, token, `Authorization` header or email.
16. The behavior and tests of `GET /api/v1/pokemon` and `GET /api/v1/pokemon/{idOrName}` do not change (stage notes).
17. The email is trimmed and lowercased in the application service before it is stored or looked up; responses show the stored email (Q1).
18. Register also rejects a password longer than **72 UTF-8 bytes** → 400 on `password`, message "size must be at most 72 bytes" (Q2).
19. The password is never trimmed, on register or login; only `name` and `email` are trimmed (Q3).
20. `SecurityConfigTest.shouldDenyAccessWhenRouteIsNotPublicAndRequestHasNoToken` becomes `shouldReturnUnauthorizedWhenRouteIsNotPublicAndRequestHasNoToken`, expecting 401 (Q4).

## Out of scope

- Roles, refresh tokens, logout, password recovery, `/me` (requirements "Not asked for by the PDF"; `decisions.md` "Out of scope").
- Seed users / demo credentials (S7).
- Any `/api/v1/local/pokemon` controller (US03 to CRUD).
- Account lockout, rate limiting, password strength rules beyond the contract.

## Decisions taken by the agent

Implementation details answered by the docs or the reference patterns.

| # | Decision | Justification |
|---|---|---|
| A1 | Token lifetime **3600 s**, configurable as `security.jwt.expiration: 1h`; a zero or negative value fails at startup (same fail-fast style as `pokeapi.max-concurrent-calls`). | Contract example `"expiresIn": 3600`. |
| A2 | **HS256** (HMAC-SHA256) with a symmetric key read from `security.jwt.secret: ${JWT_SECRET}`; the key is the UTF-8 bytes of the value and must be **at least 32 bytes**, or the application fails at startup with a message that never prints the value. | Stage notes: one signing key from an environment variable → a shared secret, not a key pair. 32 bytes (256 bits) is the HS256 minimum in RFC 7518 §3.2, which Nimbus enforces. |
| A3 | The key is read from a **simulated vault**: `spring.config.import: optional:configtree:./vault/` turns each file in `backend/vault/` into a property, so `backend/vault/JWT_SECRET` resolves `security.jwt.secret: ${JWT_SECRET}`. The `JWT_SECRET` environment variable overrides it. Without either, the application does not start. The vault path is relative to the **working directory**: start the application from `backend/` (`java -jar web/target/...`); `spring-boot:run` and the IDE default to `backend/web` and would not find it (S7: README and Dockerfile must account for this). Tests read a test-only key from `web/src/test/resources/config/application.yaml`; infrastructure tests pass it as a test property. | Developer decision, 2026-10-03: the secret lives outside `application.yaml`, the way Vault/Kubernetes mount secrets as files. |
| A4 | JWT claims: `sub` = user id, `iat`, `exp`. No email in the token, no issuer/audience. The decoder checks signature, algorithm and `exp` (Spring's default validator, 60 s clock skew). | Minimum needed to authenticate; the email is personal data (CLAUDE.md "never log emails"). |
| A5 | The `JwtEncoder` and `JwtDecoder` beans live in one `JwtConfig` (`infrastructure/user/security/token`), so the key is parsed in one place. `SecurityConfig` imports it, so web slice tests that import `SecurityConfig` (the reference pattern) get the decoder without extra setup. | D-07; "Do not create new packages"; keeps the US01/US02 web tests untouched. |
| A6 | 401 on protected routes is built by `GlobalExceptionHandler` (`@ExceptionHandler(AuthenticationException.class)`): `SecurityConfig` sets an entry point that delegates to Spring MVC's `HandlerExceptionResolver`. `detail` "Authentication required", header `WWW-Authenticate: Bearer` without the default `error_description` (it would say why the token failed). | D-17 (single `@RestControllerAdvice`); rule 4 (one generic message). |
| A7 | Login failure: domain `InvalidCredentialsException` → 401 `detail` "Invalid email or password", identical for unknown email and wrong password. | Contract "Auth"; rule 9. |
| A8 | A bearer token sent to a **public** route is ignored (a `BearerTokenResolver` returns no token for the public matchers), so an expired token never turns a public request into a 401. | Rule 16: Spring's default would answer 401 on `GET /api/v1/pokemon` with a stale token, changing US01/US02 behavior. |
| A9 | `EmailAlreadyUsedException` is thrown by the service after `existsByEmail`, and also by the persistence adapter when the `uk_users_email` constraint fails (`DataIntegrityViolationException` from `saveAndFlush` whose Hibernate `ConstraintViolationException` names that constraint, for two concurrent registrations). Any other integrity violation is rethrown unchanged (A10). The adapter's `WARN` omits the cause: H2's message contains the duplicated email. | CLAUDE.md "Adapters translate framework exceptions"; "never log emails". |
| A10 | Other `DataAccessException`s are not translated: they are unexpected failures answered by Spring's default error handling (500, no stack trace). | US02 Q8 ("no catch-all handler"); the contract lists no 5xx for auth. |
| A11 | Body validation errors: `GlobalExceptionHandler` overrides `handleMethodArgumentNotValid` to answer "Validation failed" + `errors[]`. Malformed JSON keeps Spring's fixed `detail` ("Failed to read request") with `type` `about:blank`. | Contract "Errors"; D-17. |
| A12 | Trimming: request records trim `name` and `email` in their compact constructor, which runs before Bean Validation. | Contract general rule "Text fields are trimmed before validation". |
| A13 | `@Email` (Hibernate Validator) for "valid email format"; `@NotBlank` + `@Size` for required and length rules; all messages written in English. | D-27; CLAUDE.md "Language". |
| A14 | The login `email` follows the same `email` rule as register (format, ≤ 254); the login `password` is only `@NotBlank`. | The contract's `email` row is not limited to register; the password length row is "(register)". |
| A15 | Table `users`: `id BIGINT GENERATED BY DEFAULT AS IDENTITY` (PK), `name VARCHAR(100)`, `email VARCHAR(254)` with constraint `uk_users_email`, `password_hash VARCHAR(60)` (BCrypt length), all `NOT NULL`. File `V1__create_users_table.sql`. JPA `ddl-auto: validate`, so tests fail if the entity and the migration diverge. | D-14; stage notes (only the needed columns); D-27 lengths. |
| A16 | BCrypt with the library default strength (10). | No value in the docs; the Spring Security default. |
| A17 | Domain `User` record (`id`, `name`, `email`, `passwordHash`): `id` is `null` before saving and positive after; `name`, `email`, `passwordHash` not blank. Its `toString` and the `toString` of the commands and request records hide the password / hash. | "Invariants in domain"; "passwords never logged" (a record's default `toString` prints every component). |
| A18 | Use cases: `RegisterUserUseCase` → `UserResult`, `LoginUseCase` → `AccessTokenResult` (`value`, `expiresInSeconds`); `TokenPort.issue(User)` returns `AccessTokenResult`. `tokenType` `"Bearer"` is a MapStruct constant in `web`. | "One use case per interface"; `command`/`result` records (`architecture.md`); the result must not carry the hash. |
| A19 | No `Location` header on register. | The contract gives `201 + Location` only to `POST /api/v1/local/pokemon`; there is no route to read a user. |
| A20 | One `@SpringBootTest` flow test (register → login → protected route with the token) in addition to the slice tests. | Proves that a token issued by `JwtTokenAdapter` is accepted by the resource server, with the real BCrypt, Flyway and H2. |
| A21 | `logging.level.org.hibernate.orm.jdbc.error: ERROR` in `application.yaml`. | Found in the build log: Hibernate's SQL error logger writes a `WARN` with the values of a unique-constraint violation, i.e. the duplicated email (CLAUDE.md "never log emails"). |

## Open questions

Real decisions, asked to the developer on 2026-10-03. All four answered with the recommendation (now rules 17–20).

1. **Email case.** The docs do not say whether `Ash@Example.com` and `ash@example.com` are the same account. **Recommendation:** trim and lowercase the email in the application service before storing and before looking it up (like `idOrName` in US02); the response shows the stored (lowercase) email.
2. **Password limit: chars or bytes.** The contract says "8 to 72 chars (BCrypt only uses the first 72 bytes)". Spring Security 7.1.1 `BCrypt.hashpw` **throws** `IllegalArgumentException` ("password cannot be more than 72 bytes") when hashing a longer password (verified in the 7.1.1 sources), so a 30-char password of 4-byte characters passes `@Size(max = 72)` and would answer 500. **Recommendation:** keep `@Size(min = 8, max = 72)` and add a small Bean Validation constraint in `web/user/dto/request` that also rejects more than 72 UTF-8 bytes → 400 on `password`, message "size must be at most 72 bytes". Login is not affected (checking a long password does not throw; it just never matches → 401).
3. **Password trimming.** The general rule "text fields are trimmed before validation" would silently change a password with leading/trailing spaces. **Recommendation:** never trim `password` (register or login); trim only `name` and `email`.
4. **Existing test from US02.** `SecurityConfigTest.shouldDenyAccessWhenRouteIsNotPublicAndRequestHasNoToken` expects **403** for `/api/v1/local/pokemon` without a token (its comment says "403 until AUTH (S3) adds the JWT resource server, whose entry point answers 401"). The stage notes require 401. **Recommendation:** change that expectation to 401 (and rename it to `shouldReturnUnauthorizedWhenRouteIsNotPublicAndRequestHasNoToken`); no other US01/US02 test changes.

Open after the final review (2026-10-03), waiting for the developer:

5. **Vault in git.** `backend/vault/JWT_SECRET` is untracked and not ignored, so `git add` commits it. Commit it (anyone can run the clone, the key is public) or add `vault/` to `.gitignore`. Either way, record it as a D-xx in `docs/decisions.md`, since it is an exception to CLAUDE.md "No secrets in code".
6. **Fixed vault location.** Optionally make `spring-boot:run` / the IDE find the vault (for example `<workingDirectory>` on `spring-boot-maven-plugin`, or an absolute path). Config change, needs approval.
7. **Non-final field in `MaxUtf8BytesValidator`.** `max` is set in `ConstraintValidator.initialize`; add that case to the "`final` everywhere" exceptions in `docs/architecture.md`?
8. **Lowercased email longer than 254.** `toLowerCase` can lengthen a string (`İ` → 2 chars), so a 254-char address with such characters would overflow `VARCHAR(254)` and answer 500. Edge case; a length invariant in domain `User` would cover it.
9. **`spring.jpa.open-in-view`.** Every startup logs a `WARN` that it is enabled by default; setting it to `false` silences it and stops holding a session per request.

Suggestions not implemented (ideas nobody asked for):

- **Login timing.** An unknown email skips the BCrypt check, so it answers faster than a wrong password. A dummy hash check would hide that, but registration already reveals whether an email exists (409, in the contract), so it adds little.

## Tasks

Each task takes ≤ 45 min of AI work and becomes one `test:` commit plus one `feat:` commit. Inside-out order:

- [x] **T1 domain:** `user.model.User`, `user.exception.EmailAlreadyUsedException`, `user.exception.InvalidCredentialsException`. Tests first: invariants (null id allowed, non-positive id, blank name/email/hash rejected), `toString` hides the hash, fixed exception messages without the email.
- [x] **T2 application:** `RegisterUserUseCase`, `LoginUseCase`, `UserRepositoryPort`, `PasswordHasherPort`, `TokenPort`, `RegisterUserCommand`, `LoginCommand`, `UserResult`, `AccessTokenResult`, `RegisterUserService`, `LoginService`. Tests first with mocked ports: register hashes and saves (normalized email per Q1), returns no hash, duplicate → 409 exception without hashing or saving; login returns the token, unknown email and wrong password → the same `InvalidCredentialsException`; commands hide the password in `toString`.
- [x] **T3 infrastructure, persistence:** `V1__create_users_table.sql`, `UserEntity`, `UserJpaRepository`, `UserPersistenceMapper`, `UserPersistenceAdapter`, `ddl-auto: validate`. `@DataJpaTest` on H2 in memory: save assigns an id, find/exists by email, duplicate email → `EmailAlreadyUsedException`, the migration matches the entity.
- [x] **T4 infrastructure, security:** `BCryptPasswordHasher`, `JwtConfig`, `JwtTokenAdapter`, `security.jwt.*` properties. Tests: hash is BCrypt and salted, `matches` true/false; issued token decodes with the configured decoder (`sub`, `exp - iat` = 3600, HS256), `expiresInSeconds` 3600; a missing or short secret fails at startup.
- [x] **T5 web, auth endpoints:** `AuthController`, `RegisterRequest`, `LoginRequest`, `UserResponse`, `TokenResponse`, `AuthMapper`, `UserUseCaseConfig`, `GlobalExceptionHandler` (409, 401 login, body validation `errors[]`). `@WebMvcTest`: contract JSON, every validation rule, trimming, 409, 401 generic, malformed JSON, no password in any response.
- [x] **T6 web, protected routes:** `SecurityConfig` with the resource server, entry point (A6), token resolver for public routes (A8), `GlobalExceptionHandler` 401; test-only key file. `SecurityConfigTest`: `/api/v1/local/pokemon` without token / invalid / expired → 401 `ProblemDetail`; valid → passes security; public catalog with an invalid token → 200. `@SpringBootTest` flow test (A20). Context test still green.
