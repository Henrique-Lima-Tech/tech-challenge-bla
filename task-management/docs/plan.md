# Task Management API — Implementation Plan (Phase 1)

Status: **questions answered (round 1), awaiting approval to start T01**. No production code written.

Starting point read: single-module Initializr POM (`com.challenge.aitools:task-management:0.0.1-SNAPSHOT`,
parent `spring-boot-starter-parent:4.1.1`, `java.version 25`), `spring-boot-starter` +
`spring-boot-starter-test`, `TaskManagementApplication` in `com.challenge.aitools.task_management`,
`application.yaml` with only `spring.application.name`, and `TaskManagementApplicationTests.contextLoads`.
Maven wrapper 3.9.16, JDK 25.0.4. Machine locale is `pt_BR`, which confirms the explicit-English
Bean Validation messages rule.

---

## 1. API contract

All routes are under `/api/v1`. All request and response bodies are `application/json`.
Every error body is a `ProblemDetail` (section below).

### POST /api/v1/auth/register — public

Request:

```json
{ "name": "Demo User", "email": "demo@example.com", "password": "password123" }
```

Response `201 Created`:

```json
{ "id": 1, "name": "Demo User", "email": "demo@example.com" }
```

| Status | When |
|---|---|
| 201 | created |
| 400 | validation failed, malformed JSON, empty body, unknown body field |
| 409 | email already registered |

No `Location` header: there is no route that returns a user by id (see Open question Q1).

### POST /api/v1/auth/login — public

Request:

```json
{ "email": "demo@example.com", "password": "password123" }
```

Response `200 OK`:

```json
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9..." }
```

| Status | When |
|---|---|
| 200 | authenticated |
| 400 | validation failed, malformed JSON, empty body, unknown body field |
| 401 | any login failure (unknown email, wrong password) — one generic body |

### POST /api/v1/tasks — requires token

Request (`status` optional, defaults to `TODO`; `description` optional):

```json
{ "title": "Write the plan", "description": "Phase 1", "status": "TODO", "dueDate": "2026-10-31" }
```

Response `201 Created`, header `Location: /api/v1/tasks/{id}`:

```json
{
  "id": 7,
  "title": "Write the plan",
  "description": "Phase 1",
  "status": "TODO",
  "dueDate": "2026-10-31",
  "createdAt": "2026-10-05T12:00:00Z",
  "updatedAt": "2026-10-05T12:00:00Z"
}
```

| Status | When |
|---|---|
| 201 | created |
| 400 | validation failed, `dueDate` in the past, unknown status, malformed JSON, empty body, `id`/`ownerId`/`createdAt`/`updatedAt` in the body |
| 401 | missing, invalid or expired token |

### GET /api/v1/tasks — requires token

Query parameters: `page` (integer, >= 0, default 0), `size` (integer, 1..100, default 20),
`status` (optional, one of `TODO`, `IN_PROGRESS`, `DONE`).
Ordering is always `dueDate` ascending, then `id` ascending.

Response `200 OK`:

```json
{
  "content": [ { "id": 7, "title": "...", "description": null, "status": "TODO",
                 "dueDate": "2026-10-31", "createdAt": "...", "updatedAt": "..." } ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

| Status | When |
|---|---|
| 200 | always, including a page past the end (`content: []`, real `totalElements`/`totalPages`) |
| 400 | non-integer or out-of-range `page`/`size`, unknown `status` |
| 401 | missing, invalid or expired token |

### GET /api/v1/tasks/{id} — requires token

Response `200 OK`: the task body shown above.

| Status | When |
|---|---|
| 200 | the task belongs to the caller |
| 400 | `{id}` is not an integer, or is lower than 1 |
| 401 | missing, invalid or expired token |
| 404 | no such task **or** it belongs to another user |

### PUT /api/v1/tasks/{id} — requires token

Full replacement of `title`, `description`, `status` and `dueDate`.

```json
{ "title": "New title", "description": null, "status": "DONE", "dueDate": "2026-11-30" }
```

Response `200 OK`: the updated task body.

| Status | When |
|---|---|
| 200 | replaced |
| 400 | validation failed, changed `dueDate` in the past, unknown status, malformed JSON, empty body, read-only field in the body |
| 401 | missing, invalid or expired token |
| 404 | no such task, or it belongs to another user |

### DELETE /api/v1/tasks/{id} — requires token

Response `204 No Content`, empty body.

| Status | When |
|---|---|
| 204 | deleted |
| 400 | `{id}` is not an integer, or is lower than 1 |
| 401 | missing, invalid or expired token |
| 404 | no such task, or it belongs to another user |

### Error bodies

One `@RestControllerAdvice`. `type` is always `about:blank`; `instance` is the request path.

| Case | Status | `detail` |
|---|---|---|
| Bean Validation / parameter validation | 400 | `Validation failed` + `errors[]` of `field`, `message` |
| Malformed JSON, or an empty body | 400 | `Malformed request body` |
| Unknown or read-only body field (`id`, `ownerId`, `createdAt`, `updatedAt`, anything unmapped) | 400 | `Validation failed` + `errors[]`, `message` `must not be sent` |
| Business invariant broken in the domain | 400 | `Validation failed` + `errors[]` |
| Login failure | 401 | `Invalid email or password` |
| Missing, invalid or expired token | 401 | `Authentication required` (+ `WWW-Authenticate: Bearer`) |
| Task not found or owned by another user | 404 | `Task not found` |
| Email already registered | 409 | `Email already registered` |

Validation example:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "instance": "/api/v1/tasks",
  "detail": "Validation failed",
  "errors": [ { "field": "title", "message": "must not be blank" } ]
}
```

A read-only or unknown field uses the same shape (Q6):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "instance": "/api/v1/tasks",
  "detail": "Validation failed",
  "errors": [ { "field": "ownerId", "message": "must not be sent" } ]
}
```

No body ever carries a stack trace, a database message or the rejected value.

---

## 2. Rules

```
R-001 — Four Maven modules under a parent POM: domain, application, infrastructure, web.
        web -> application + infrastructure; infrastructure -> application; application -> domain.
        Source: "Architecture". Tests: the build itself (./mvnw verify).

R-002 — The dependency rule is enforced only by the module POMs: an illegal import in domain or
        application does not compile. No architecture test, no other mechanism.
        Source: "Architecture", "Constraints". Tests: the build itself.

R-003 — Packages are com.challenge.aitools.taskmanagement.<layer>.<feature>, features task, user,
        shared. The generated package underscore is dropped.
        Source: "Architecture". Tests: the build itself.

R-004 — The Initializr POM becomes the parent; the main class, application.yaml and the context-load
        test move to web, the only executable module.
        Source: "The starting point", "Architecture". Tests: TaskManagementApplicationTests.

R-005 — domain is plain Java: no Spring, JPA, Jackson or Bean Validation. application is plain Java:
        no Spring, no JPA. Lombok is allowed everywhere; MapStruct only in infrastructure and web.
        Source: "Domain module", "Application module", "Code conventions". Tests: the build itself.

R-006 — A user is id, name, unique email, passwordHash. No roles, refresh token, logout or password
        recovery.
        Source: "User and authentication rules". Tests: UserTest.

R-007 — Registration takes name, email and password; the password is at least 8 characters and at
        most 72 UTF-8 bytes, the part BCrypt considers.
        Source: "User and authentication rules" + Q3 of docs/open-decisions.md.
        Tests: AuthControllerTest.

R-008 — Email is normalised for comparison and storage: surrounding whitespace trimmed, compared
        case-insensitively.
        Source: "User and authentication rules". Tests: UserTest, RegisterUserServiceTest,
        LoginServiceTest, UserRepositoryAdapterTest.

R-009 — Registering an email that already exists responds 409.
        Source: "Edge cases". Tests: RegisterUserServiceTest, AuthControllerTest.

R-010 — Login returns an accessToken: a JWT, HS256, valid for one hour, signed with a key taken from
        an environment variable. No secret in the code.
        Source: "User and authentication rules", "Security". Tests: JwtTokenIssuerTest,
        AuthFlowIntegrationTest.

R-011 — Any login failure responds the same generic 401, with no hint of which part failed.
        Source: "Edge cases". Tests: LoginServiceTest, AuthControllerTest.

R-012 — A task is id, title, description, status, dueDate, createdAt, updatedAt and an owner.
        Source: "Task rules". Tests: TaskTest.

R-013 — title is required, trimmed, 1 to 120 characters; description is optional, trimmed, stored
        as null when blank, up to 2000 characters.
        Source: "Task rules" + Q5 of docs/open-decisions.md. Tests: TaskTest, TaskControllerTest.

R-014 — status is TODO, IN_PROGRESS or DONE, defaults to TODO when not provided, and moves freely
        between the three values: no transition rule.
        Source: "Task rules". Tests: TaskTest, UpdateTaskServiceTest, TaskControllerTest.

R-015 — dueDate is required, ISO format, and may not be in the past on creation. On update it is
        validated only when it changes, so keeping an already overdue date is allowed.
        Source: "Task rules". Tests: TaskTest, CreateTaskServiceTest, UpdateTaskServiceTest.

R-016 — "Today" always comes from the injected Clock port, never from the system clock directly, so
        tests control time.
        Source: "Task rules". Tests: CreateTaskServiceTest, UpdateTaskServiceTest, SystemClockTest.

R-017 — createdAt and updatedAt are set by the server and are read-only; the owner always comes from
        the token, never from the request body.
        Source: "Task rules". Tests: TaskTest, TaskControllerTest.

R-018 — Each use case has an interface as its input port and a service implementing it:
        RegisterUser, Login, CreateTask, ListTasks, GetTask, UpdateTask, DeleteTask. Every task use
        case receives the authenticated user id.
        Source: "Application module". Tests: one test class per service.

R-019 — Output ports: TaskRepository, UserRepository, PasswordHasher, TokenIssuer, Clock.
        Source: "Application module". Tests: the service tests, with Mockito on the ports.

R-020 — /api/v1/auth/register and /api/v1/auth/login are public; every task route requires a token.
        Source: "API contract". Tests: TaskControllerTest, AuthControllerTest.

R-021 — POST /api/v1/tasks responds 201 with the Location header; DELETE responds 204.
        Source: "API contract". Tests: TaskControllerTest.

R-022 — GET /api/v1/tasks is paginated: page from 0, size 1..100 default 20, optional status filter,
        ordered by dueDate ascending then id. The response is always the page shape with content,
        page, size, totalElements, totalPages.
        Source: "API contract". Tests: ListTasksServiceTest, TaskRepositoryAdapterTest,
        TaskControllerTest.

R-023 — A page past the end responds 200 with empty content and the real totals.
        Source: "Edge cases". Tests: TaskRepositoryAdapterTest, TaskControllerTest.

R-024 — A task owned by another user responds 404, never 403, and no response reveals that it
        exists. Every repository read and write is scoped by owner id.
        Source: "Edge cases", "Constraints". Tests: GetTaskServiceTest, UpdateTaskServiceTest,
        DeleteTaskServiceTest, TaskRepositoryAdapterTest, TaskControllerTest.

R-025 — A non-numeric id, or an id lower than 1, responds 400.
        Source: "Edge cases". Tests: TaskControllerTest.

R-026 — Malformed JSON or an empty body responds 400.
        Source: "Edge cases". Tests: TaskControllerTest, AuthControllerTest.

R-027 — An unknown status responds 400, naming the field.
        Source: "Edge cases". Tests: TaskControllerTest.

R-028 — id, ownerId, createdAt or updatedAt sent in the body respond 400 naming the field in
        errors[] with the message "must not be sent", in the same "Validation failed" shape as any
        other validation error. The same applies to any other unmapped field.
        Source: "Edge cases", "Constraints" + Q6 of docs/open-decisions.md.
        Tests: TaskControllerTest, AuthControllerTest.

R-029 — A missing, invalid or expired token responds 401.
        Source: "Edge cases". Tests: TaskControllerTest, AuthFlowIntegrationTest.

R-030 — Errors are ProblemDetail (RFC 9457) from a single @RestControllerAdvice, each carrying type
        (about:blank), title, status, instance and a fixed English detail; validation errors carry
        detail "Validation failed" plus errors[] of field and message. Each business exception has
        its own handler with the correct status.
        Source: "Error responses", "Spring Boot 4 specifics". Tests: GlobalExceptionHandler is
        covered through the controller tests.

R-031 — No response exposes a stack trace, a database message or the rejected value.
        Source: "Error responses". Tests: TaskControllerTest, AuthControllerTest.

R-032 — H2 in file mode for the application, H2 in memory for the tests. Flyway owns the schema and
        the seed data (one demo user and a few of that user's tasks). Hibernate only validates the
        schema (ddl-auto: validate).
        Source: "Persistence". Tests: the @DataJpaTest slices and the application start-up.

R-033 — The OAuth2 resource server validates the JWT; no custom security filter. BCrypt hashes the
        passwords.
        Source: "Security", "Constraints". Tests: TaskControllerTest, BCryptPasswordHasherTest.

R-034 — Nothing logs a password, a token, the Authorization header, an email or a request body.
        Source: "Security". Tests: reviewed in Phase 3 (no automated test).

R-035 — web validates the input format; the domain protects the invariants. Bean Validation messages
        are written explicitly in English. Adapters translate framework exceptions into domain
        exceptions. JPA entities never leave infrastructure. Application services become beans
        through configuration classes in web. Injection is always through the constructor.
        Source: "Code conventions". Tests: the adapter and controller tests.

R-036 — Every value that is not reassigned is final, in production and test code. Exceptions:
        framework-injected test fields, record components, lambda parameters.
        Source: "Code conventions". Tests: reviewed in Phase 3.

R-037 — Records for DTOs, commands and results. @Slf4j only in infrastructure and web: DEBUG for the
        normal flow, INFO for start-up facts, a single WARN with context when an external failure is
        handled. Comments only for what the code cannot say.
        Source: "Code conventions". Tests: reviewed in Phase 3.

R-038 — Everything in the project is in English, and no dependency is added beyond the approved list.
        Source: "Code conventions", "Constraints". Tests: reviewed in Phase 3.

R-039 — TDD in domain and application: failing test first, then the minimum implementation. In the
        adapters and web, tests may be written alongside the code. No test is ever deleted, disabled
        or weakened. Test names are should<Result>When<Condition>, test packages mirror production
        packages, bodies are split into // given, // when, // then.
        Source: "Tests". Tests: the whole suite.

R-040 — Out of scope: architecture tests, coverage thresholds, CI and OpenAPI documentation.
        Source: "Constraints". Tests: none.
```

---

## 3. Decisions

| # | Decision | Reason |
|---|---|---|
| D-01 | Group `com.challenge.aitools`, parent artifact `task-management` with `pom` packaging, modules `domain`, `application`, `infrastructure`, `web`. | The Initializr POM becomes the parent, as required. |
| D-02 | Sub-packages below the feature: `model`, `exception` (domain); `command`, `result`, `port.in`, `port.out`, `service` (application); `persistence`, `security` (infrastructure); `controller`, `dto.request`, `dto.response`, `mapper`, `config`, `error`, `security`, `pagination` (web). | `<layer>.<feature>` is the required prefix; these keep each feature readable without breaking it. |
| D-03 | Timestamps are `Instant`; `dueDate` is a `LocalDate`. "Today" is derived from the `Clock` port's `Instant` at UTC. | One time type on the wire and in the database; no timezone configuration is in scope. |
| D-04 | The `Clock` output port exposes `Instant now()`. The domain receives `now` as a parameter and derives today itself. | Matches the given `Task.create(..., now)` signature; keeps the domain free of ambient time. |
| D-05 | A framework-free `Page<T>` record in `application.shared.pagination`, and a `PageResponse<T>` record in `web.shared.pagination`. | Spring's `Page` cannot cross into `application`, and the JSON shape is fixed by the contract. |
| D-06 | `status` arrives as a `String` in request DTOs and query parameters, validated with `@Pattern(regexp = "TODO\|IN_PROGRESS\|DONE")`, and is converted to the enum in `web`. | An unknown status then produces a normal field error that names the field (R-027), instead of a Jackson deserialisation failure. |
| D-07 | `spring.jackson.deserialization.fail-on-unknown-properties: true`; the `HttpMessageNotReadableException` handler inspects the cause: an unrecognised property becomes 400 `Validation failed` with `errors: [{field, "must not be sent"}]`, anything else becomes 400 `Malformed request body`. | Q6: the offending field is named, like an unknown status, while malformed JSON and an empty body keep a fixed detail and leak nothing. Reading the field path out of the Jackson 3 exception is confirmed by compiling in T10. |
| D-08 | Register responds 201 with `{id, name, email}` and no `Location` header. | 201 fits a creation; there is no route that returns a user, so a `Location` would point nowhere. See Q1. |
| D-09 | Task responses omit `ownerId`. | The owner is always the caller; echoing it adds nothing and keeps ownership out of the wire. See Q2. |
| D-10 | The password rule lives in `web` Bean Validation: `@Size(min = 8)` plus a small custom constraint for the 72-UTF-8-byte ceiling. | The domain only ever holds the hash, so it cannot own this rule. Q3 asks for a byte ceiling, and Bean Validation has no built-in annotation for it; a local constraint needs no new dependency. |
| D-11 | Email normalisation (trim + lowercase) is a static helper on the domain `User`, applied on registration and before every lookup. | One normalisation point, reachable from both the domain and the use cases. |
| D-12 | Schema in `classpath:db/migration`, seed data in `classpath:db/seed`. The application loads both; `web` tests and the `infrastructure` `@DataJpaTest` slices load only `db/migration`. | Tests assert on data they create themselves; the demo data must not leak into them. |
| D-13 | The demo user is `demo@example.com` / `demo1234`, its BCrypt hash written into the seed migration, with five tasks across the three statuses and due dates relative to `CURRENT_DATE`. The credentials go in the README. | Q9. Relative dates keep the seed from going stale: the overdue task stays overdue and the future ones stay in the future, however long after today the project is run. |
| D-14 | `security.jwt.secret: ${JWT_SECRET}` with no default, `security.jwt.expiration: 1h`. Tests get a key from `web/src/test/resources/config/application.yaml`. | No secret in the code, and the application refuses to start without the variable. Spring Boot loads `config/application.yaml` from the classpath for every test without a profile. |
| D-15 | The owner id is read from the JWT `sub` claim through `@AuthenticationPrincipal Jwt`, with one small helper in `web.shared.security`. | The owner comes from the token only (R-017), with no custom filter. |
| D-16 | Tables `users` and `tasks`; `tasks` has an index on `(owner_id, due_date, id)`. | Matches the only ordering and the only filter in the contract. |
| D-17 | MapStruct pinned to 1.6.3, `lombok-mapstruct-binding` 0.2.0, processor order Lombok → MapStruct → binding in `pluginManagement`. | The order required by Boot 4; these exact versions are already proven on Boot 4.1.1 / JDK 25 in the sibling `backend/` module of this repository. |
| D-18 | Dependencies per module: `domain` — lombok, junit-jupiter, assertj, mockito; `application` — domain + the same test set; `infrastructure` — application, `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway`, `spring-boot-starter-security-oauth2-resource-server`, h2 (runtime), mapstruct, lombok, `spring-boot-starter-data-jpa-test`; `web` — application, infrastructure, `spring-boot-starter-webmvc`, `spring-boot-starter-validation`, `spring-boot-starter-security-oauth2-resource-server`, mapstruct, lombok, `spring-boot-starter-webmvc-test`, `spring-boot-starter-security-oauth2-resource-server-test`. | Exactly the listed stack. BCrypt and the Nimbus JWT encoder/decoder both arrive with the resource-server starter, so nothing extra is needed. |
| D-19 | `spring-boot-starter-test` from the Initializr POM is replaced by the split Boot 4 test starters per module. | Boot 4 splits the test starters by technology. |

---

## 4. Resolved questions (round 1)

Answered by the developer in `open-decisions.md`, which is the record for this round. Four
answers differ from the proposals in the plan and are marked **changed**; the rules, decisions and
tasks above already reflect all nine.

| # | Question | Answer | Effect |
|---|---|---|---|
| Q1 | Register response | 201 with `{id, name, email}`, no `Location` | as proposed — D-08 |
| Q2 | `ownerId` in task responses | left out, the owner is always the logged-in user | as proposed — D-09 |
| Q3 | Password length | at least 8 characters, at most 72 **UTF-8 bytes** | **changed** — R-007, D-10; adds a custom constraint in T11 |
| Q4 | User field limits | `name` 100, `email` 254, `password_hash` 60 | as proposed — T07 schema |
| Q5 | `description` normalisation | trimmed, a blank value stored as `null` | **changed** — R-013, a domain invariant in T03 |
| Q6 | Read-only fields in the body | name the field in `errors[]` with `must not be sent`, in the `Validation failed` shape | **changed** — R-028, D-07, T10 |
| Q7 | "Not in the past" boundary | UTC | as proposed — D-03, D-04 |
| Q8 | `size` above the range | 400, like any other invalid value | as proposed — R-022 |
| Q9 | Seed data | `demo@example.com` / `demo1234`, five tasks across the three statuses, due dates relative to the current date | **changed** — D-13; the seed moves from T07 to T09 |

Good ideas deliberately left out of scope: OpenAPI documentation, architecture tests (ArchUnit),
a coverage threshold, CI, a `GET /api/v1/users/me` route, soft delete, and a `PATCH` for partial task
updates. None of them will appear in the code.

---

## 5. Implementation tasks

Each task ends with a green module build, the changed files, and suggested commit messages.
`domain` and `application` tasks are strict TDD.

| # | Task | Scope | Tests added |
|---|---|---|---|
| T01 | Multi-module skeleton | Parent POM (`pom` packaging, modules, `dependencyManagement`, processor order), four module POMs with only what T02–T04 need, `TaskManagementApplication` + `application.yaml` + the context test moved to `web`, package renamed to `...taskmanagement.web`. Spring deps that need configuration (JPA, Flyway, security) arrive with their own task, so the build stays green. | The existing `contextLoads`, now in `web`. |
| T02 | Domain: `User` | `user.model.User` with the email normalisation helper, `user.exception.InvalidUserException`, `EmailAlreadyRegisteredException`, `InvalidCredentialsException`. R-006, R-008, R-009, R-011. | `UserTest` |
| T03 | Domain: `TaskStatus` + `Task` | `task.model.TaskStatus`, `task.model.Task` with `create`, the replacement operation, the invariants, the `title`/`description` normalisation and the `dueDate` rule; `task.exception.InvalidTaskException`, `TaskNotFoundException`. R-012 to R-017. | `TaskTest` |
| T04 | Application: user ports and use cases | `shared.port.out.Clock`; `user.port.out.UserRepository`, `PasswordHasher`, `TokenIssuer`; `user.port.in.RegisterUser`, `Login`; commands, results and the two services. R-007 to R-011, R-018, R-019. | `RegisterUserServiceTest`, `LoginServiceTest` |
| T05 | Application: task write use cases | `shared.pagination.Page`; `task.port.out.TaskRepository`; `CreateTask`, `GetTask`, `DeleteTask` with their commands, results and services. R-015, R-016, R-018, R-019, R-024. | `CreateTaskServiceTest`, `GetTaskServiceTest`, `DeleteTaskServiceTest` |
| T06 | Application: `UpdateTask` and `ListTasks` | The two remaining services, including the "validate `dueDate` only when it changes" rule and the paging/filtering query. R-014, R-015, R-022, R-024. | `UpdateTaskServiceTest`, `ListTasksServiceTest` |
| T07 | Infrastructure: schema | `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway` and h2 added; `V1__create_users_table.sql`, `V2__create_tasks_table.sql`; datasource, Flyway locations and `ddl-auto: validate` in `application.yaml`. The seed moves to T09, which is where BCrypt exists to produce the demo hash. R-032, R-016 (index), D-12. | Covered by T08's slices and by the application start-up. |
| T08 | Infrastructure: persistence adapters | `UserEntity`, `TaskEntity`, MapStruct mappers, Spring Data repositories, `UserRepositoryAdapter`, `TaskRepositoryAdapter` (owner-scoped reads and writes, paging, ordering, `DataAccessException` translated to domain exceptions). R-008, R-022, R-023, R-024, R-035. Confirms by compiling and running that `Instant` matches the migration's timestamp type under `ddl-auto: validate`. | `UserRepositoryAdapterTest`, `TaskRepositoryAdapterTest` (`@DataJpaTest`) |
| T09 | Infrastructure: security, clock and seed | `spring-boot-starter-security-oauth2-resource-server` added; `BCryptPasswordHasher`, `JwtConfig` (HS256 encoder + decoder from `${JWT_SECRET}`), `JwtTokenIssuer` (one-hour expiry, `sub` = user id), `SystemClock`; `security.jwt.*` properties and the test key; `db/seed/V3__insert_demo_data.sql` with the hash produced by the hasher itself. R-010, R-016, R-032, R-033, D-13, D-14. | `BCryptPasswordHasherTest`, `JwtTokenIssuerTest`, `SystemClockTest` |
| T10 | Web: shared plumbing | `spring-boot-starter-validation` and the security starters added; `SecurityConfig` (public auth routes, everything else authenticated, 401 routed through the advice), `GlobalExceptionHandler`, `PageResponse`, the current-user helper, and the `@Configuration` classes that register the seven services as beans. R-020, R-026, R-028, R-029, R-030, R-031, R-035, D-07. | A slice test for the 401 cases, the read-only-field rejection and the `ProblemDetail` shape |
| T11 | Web: authentication endpoints | `AuthController`, `RegisterRequest`, `LoginRequest`, `UserResponse`, `TokenResponse`, the MapStruct mapper, and the custom `@MaxUtf8Bytes` constraint with its validator. R-007, R-009, R-011, R-020, R-026, R-028. | `AuthControllerTest` (`@WebMvcTest`, `@MockitoBean` on the use cases), `MaxUtf8BytesValidatorTest` |
| T12 | Web: task endpoints | `TaskController` (five routes), `CreateTaskRequest`, `UpdateTaskRequest`, `TaskResponse`, the mapper, the paging and filter parameters with their bounds. R-013, R-014, R-021 to R-028, R-031. | `TaskControllerTest` (`@WebMvcTest`): statuses, JSON, validation, security and every edge case |
| T13 | Integration tests | `@SpringBootTest` walking registration → login → create → list → get → update → delete, plus another user's task answering 404 and an expired/invalid token answering 401. R-010, R-024, R-029. | `AuthFlowIntegrationTest`, `TaskCrudIntegrationTest` |
| T14 | Phase 3 | Full `./mvnw verify`, the `curl` session against the running application, the code review, the README (setup, demo credentials, routes, how to run the tests) and the honest validation report. | None |

### Known risks, to be confirmed by compiling or running, never assumed

- The H2 column type that `ddl-auto: validate` accepts for an `Instant` (expected
  `TIMESTAMP(6) WITH TIME ZONE`) is confirmed in T07/T08 by running the slice, not from Boot 3 habits.
  The sibling `backend/` module has no timestamp column, so there is no proven precedent in this
  repository.
- The Jackson 3 exception type that carries the rejected property name (expected
  `tools.jackson.databind.exc.UnrecognizedPropertyException`, reached through the cause of
  `HttpMessageNotReadableException`) is confirmed by compiling in T10. Q6 needs the field name, so if
  that type or its path accessor differs, I stop and report instead of guessing.
- The H2 expression for a date relative to today in the seed migration (expected
  `DATEADD('DAY', n, CURRENT_DATE)`) is confirmed in T09 by running Flyway.
