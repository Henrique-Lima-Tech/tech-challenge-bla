# Validation report — Phase 3

Date: 2026-10-05. Everything below was run; nothing is claimed from reading the code alone.
The application was exercised with a 39-call `curl` session against the running jar.

## 1. Build

`./mvnw clean verify` from `task-management/`: **BUILD SUCCESS in 24 s, 181 tests, 0 failures,
0 errors, 0 skipped.**

| Module | Tests |
|---|---|
| domain | 54 |
| application | 27 |
| infrastructure | 30 |
| web | 70 |

No test was deleted, disabled or weakened at any point. The `@Disabled` annotation does not appear in
the project.

## 2. What was verified, and how

The application was started as the packaged jar on port 8081 (port 8080 is taken on this machine by
another project), with `JWT_SECRET` from the environment, against H2 in file mode with the Flyway seed.

- **Flyway owns the schema and the seed — verified.** Start-up applied `V1 create users table`,
  `V2 create tasks table` and `V3 insert demo data`, and `ddl-auto: validate` passed, so the entities
  match the migrations. Querying the H2 file directly returned one user with a 60-character hash and
  five tasks at `CURRENT_DATE -10, -3, +1, +7, +14`.
- **The `Instant` column type was confirmed by running, not assumed.** `TIMESTAMP(6) WITH TIME ZONE`
  is accepted by `ddl-auto: validate` and round-trips to the microsecond:
  `TaskRepositoryAdapterTest.shouldKeepEveryFieldWhenRoundTrippingATask` asserts
  `createdAt == 2026-10-01T08:30:15Z` after a save and a reload. This was the first risk named in the
  plan.
- **The Jackson 3 exception type was confirmed by compiling, not assumed.**
  `tools.jackson.databind.exc.UnrecognizedPropertyException` with `getPropertyName()` exists in
  jackson-databind 3.1.5 and arrives as the cause of `HttpMessageNotReadableException`. This was the
  second risk named in the plan, and it is what makes answer Q6 possible.
- **The H2 relative-date syntax was confirmed by running.** `DATEADD('DAY', n, CURRENT_DATE)` applied
  cleanly; this was the third risk named in the plan.
- **Login with the demonstration credentials — verified.** `demo@example.com` / `demo1234` returned a
  three-part HS256 JWT; the token then opened `GET /api/v1/tasks`.
- **Ordering — verified.** The five seeded tasks came back ordered by `dueDate` ascending
  (2026-09-25, 10-02, 10-06, 10-12, 10-19), and `TaskRepositoryAdapterTest
  .shouldOrderTasksByDueDateThenIdWhenListing` covers the `id` tie-breaker with two tasks on the same
  date.
- **The whole CRUD — verified.** `POST` answered `201` with `Location: /api/v1/tasks/6`, `GET` returned
  it, `PUT` replaced every editable field and moved `updatedAt` while keeping `createdAt`, `DELETE`
  answered `204` with an empty body, and the following `GET` answered `404`.
- **The update rule for `dueDate` — verified both ways on real data.** Seeded task 2 is overdue
  (`2026-10-02`). Sending it back unchanged with `status: DONE` answered `200`, so an overdue task can
  still be edited and closed. Moving the same task to `2026-09-01` answered `400` with
  `errors: [{"field": "dueDate", "message": "must not be in the past"}]`.
- **Another user's task answers 404 — verified with curl.** A second user's token on the demo user's
  task 3 returned `404` with `detail` `Task not found` for `GET`, `PUT` and `DELETE`, with no ownership
  hint in any body, and `GET /api/v1/tasks` for that user returned `totalElements: 0`. Covered by
  `TaskControllerTest.shouldReturnNotFoundWhenTaskBelongsToAnotherUser` and
  `TaskCrudIntegrationTest.shouldReturnNotFoundWhenTheTaskBelongsToAnotherUser`.
- **No log carries a secret — verified at both log levels.** The session was replayed with
  `logging.level.com.challenge.aitools.taskmanagement=DEBUG`, which is the level where the normal-flow
  lines are actually emitted; the first run at the default `INFO` would not have exercised them. The
  log was then searched for every password used, the demo password, `@example.com`, `Authorization`,
  `Bearer `, the JWT prefix `eyJhbGciOi`, a task description and the signing key: **0 hits each**. The
  DEBUG lines carry ids only, for example `Creating a task for owner 1` and `Saved task 6 of owner 1`.
  No stack trace reached the log (`0` lines matching ` at package.Class`), and the only two WARN lines
  come from Flyway and Spring Boot themselves, not from this code.

## 3. Edge cases, each with its observed answer

| Edge case | Observed | Covered by |
|---|---|---|
| Another user's task | `404` `Task not found` for GET, PUT, DELETE | curl, `TaskControllerTest`, `TaskCrudIntegrationTest` |
| Non-numeric id | `400` `Validation failed`, `errors[0] = {id, must be an integer}` | curl, `TaskControllerTest` |
| Id lower than 1 | `400`, `errors[0] = {id, must be at least 1}` (0 and -1) | curl, `TaskControllerTest` |
| Malformed JSON | `400` `Malformed request body` | curl, both controller tests |
| Empty body | `400` `Malformed request body` | curl, both controller tests |
| Unknown status in the body | `400`, `{status, must be one of TODO, IN_PROGRESS, DONE}` | curl, `TaskControllerTest` |
| Unknown status in the query | same answer | curl, `TaskControllerTest` |
| `id`/`ownerId`/`createdAt`/`updatedAt` in the body | `400`, `{<field>, must not be sent}` | curl, 8 parameterized cases |
| Page past the end | `200`, `content: []`, `totalElements: 6`, `totalPages: 1` | curl, adapter and controller tests |
| Already registered email | `409` `Email already registered`, also for `CURL-USER@Example.com` | curl, `AuthControllerTest`, `AuthFlowIntegrationTest` |
| Login failure | always `401` `Invalid email or password`, for a wrong password, an unknown email and a malformed email alike | curl, `AuthControllerTest` |
| Missing token | `401` `Authentication required` + `WWW-Authenticate: Bearer` | curl, `TaskControllerTest` |
| Invalid token | same `401` | curl, `TaskControllerTest` |
| Expired token | same `401` | curl, `AuthFlowIntegrationTest` |
| `size` above 100 | `400`, `{size, must be at most 100}`; not clamped | curl, `TaskControllerTest` |
| Negative page | `400`, `{page, must be at least 0}` | curl, `TaskControllerTest` |
| Due date in the past on creation | `400`, `{dueDate, must not be in the past}` | curl, service and controller tests |
| Password under 8 characters | `400`, `{password, must be at least 8 characters}` | curl, `AuthControllerTest` |
| Password over 72 UTF-8 bytes | `400`, `{password, must be at most 72 bytes}`; 40 `é` is 40 characters but 80 bytes | `AuthControllerTest`, `MaxUtf8BytesValidatorTest` |

## 4. Code review against the requirements

Checks run over the sources:

| Requirement | Result |
|---|---|
| No Spring, JPA, Jackson or Bean Validation import in `domain` | none found |
| No Spring, JPA, Jackson, Bean Validation or MapStruct import in `application` | none found, in main or test |
| JPA entities never leave `infrastructure` | no reference to `persistence.entity` outside it |
| `@Slf4j` only in `infrastructure` and `web` | none in `domain` or `application` |
| Everything in English | the only non-ASCII characters are the `é` test data for the UTF-8 byte rule |
| No secret in the code | no literal assigned to a secret or password; `security.jwt.secret` is `${JWT_SECRET}` with no default, and the application refuses to start without it |
| `final` everywhere | every local variable is `final var`, every `catch` is `catch (final ...)`, and every parameter of every method and constructor is `final` |
| Constructor injection only | no `@Autowired` field; the Spring test classes inject through an `@Autowired` constructor into `final` fields |
| No custom security filter | the OAuth2 resource server does the validation; `SecurityConfig` adds no filter |
| No dependency beyond the approved list | the four module POMs hold exactly the approved dependencies |

The four places where mutable state remains, each with its reason:

1. `UserEntity` and `TaskEntity` fields — JPA needs a no-argument constructor and setters. The entities
   never leave `infrastructure`.
2. `MaxUtf8BytesValidator.maxBytes` — the `ConstraintValidator` contract sets it in `initialize`, after
   construction, so it cannot be `final`.
3. `TaskRepositoryAdapterTest.ownerId` and `otherOwnerId` — assigned in `@BeforeEach`, because each
   test needs its own saved owners. This is adjacent to, but not literally inside, the stated exception
   for "framework-injected fields in tests". Reported rather than hidden.

## 5. Deviations from the approved plan

Each of these is a deviation, not a silent decision:

1. **`TaskManagementApplication` sits in `com.challenge.aitools.taskmanagement`, not in `...taskmanagement.web`.** It
   still lives in the `web` module, which is what the requirement asks. Component scanning starts at
   the class's own package, so from `...web` it would never see `...infrastructure`, and the
   alternative was adding `scanBasePackages`. The root package is also what the proven sibling project
   in this repository does.
2. **`InfrastructureTestApplication` was added** (test sources only). Spring Boot test slices need a
   `@SpringBootConfiguration` in their own module, and the real one is in `web`. Without it both
   `@DataJpaTest` classes fail with "Unable to find a @SpringBootConfiguration".
3. **The domain exceptions were refactored to carry `(field, reason)`.** Answer Q6 asks for the
   offending field in `errors[]`, and the same shape then serves every domain invariant: a past due
   date comes back as `{"field": "dueDate", "message": "must not be in the past"}` instead of a bare
   400. The domain tests were not touched and stayed green.
4. **`TaskStatusPattern` was added**: two constants holding the status regexp and its message, so the
   two request records and the query parameter cannot drift apart. It is not in the plan's file list.
5. **`JwtConfigTest` was added**, which the plan did not list, to cover the minimum key length.
6. **The slice test the plan promised for T10 does not exist as its own class.** `SecurityConfig` and
   `GlobalExceptionHandler` are covered through `AuthControllerTest` and `TaskControllerTest`, which
   assert the 401, 404, 409 and 400 bodies and the `ProblemDetail` shape. Writing a separate slice test
   would have needed a controller that did not exist yet at T10.
7. **The seed moved from T07 to T09**, as the plan already recorded after the Q9 answer, because the
   demo hash has to be produced by BCrypt.

## 6. Not verified

- **Behaviour under a real concurrent load, and the H2 file database under concurrent writers.** Not
  requested and not tested.
- **A genuinely expired token.** The 401 for an expired token was exercised with a token whose `exp` is
  in the past and whose signature does not match, so the resource server rejects it before reading
  `exp`. The one-hour lifetime itself is asserted in `JwtTokenIssuerTest`
  (`exp - iat == PT1H`), not end to end: that would need to either wait an hour or make the expiry
  configurable per test, and neither is in scope.
- **Port 8080.** The demonstration ran on 8081 because another process holds 8080 on this machine. The
  default port is unchanged.
- **Anything behind a proxy, TLS, or a non-UTC server timezone.** "Today" is the UTC date by decision,
  which answer Q7 confirmed. On a machine at UTC-3 that means a due date of the local today is
  rejected between 21:00 and midnight local time. This is the agreed behaviour, not a bug, and it is
  worth remembering when demonstrating late in the evening.

## 7. Suggestions, applied to nothing

Out of scope, left out of the code on purpose, and listed only so the decision is visible:

- Spring Boot logs `spring.jpa.open-in-view is enabled by default` at start-up. Setting
  `spring.jpa.open-in-view: false` would silence it and is the usual choice for an API with no view
  rendering, but it is a change nobody asked for.
- Flyway logs that H2 2.4.240 is newer than the version it was verified with. Harmless, and pinning H2
  would mean overriding a Spring Boot managed version.

## 8. Addendum — the demonstration vault

Added after the report above, on request: a `vault/` folder holding a default `JWT_SECRET` so the
application starts as a demo with no setup. It follows the simulated-secret-store pattern already
proven in the sibling project of this repository: `spring.config.import: optional:configtree:./vault/`,
where each file name is a property key and the file content is its value.

Verified by running:

- **`./mvnw clean package` after the change: BUILD SUCCESS, the same 181 tests, 0 failures.** The
  import is `optional:` on purpose: during the tests the working directory is the module folder, where
  no `vault/` exists, so the import is a no-op and the test key keeps coming from
  `web/src/test/resources/config/application.yaml`.
- **The packaged jar started with `env -u JWT_SECRET`,** that is, with nothing in the environment, and
  served the demonstration: login with `demo@example.com` / `demo1234` returned a token, and that token
  opened `GET /api/v1/tasks` with `200`.
- **`./mvnw -pl web spring-boot:run` also started with `env -u JWT_SECRET`** and answered the same
  login with `200`.
- **`./data` now lands at the project root for both entry points.** Before this change
  `spring-boot:run` created `web/data`, because the plugin runs with the module folder as its working
  directory, while the jar created `./data` wherever it was launched. `web/pom.xml` now sets
  `<workingDirectory>${maven.multiModuleProjectDirectory}</workingDirectory>`, so `./vault` and
  `./data` resolve at the project root either way.

Not verified:

- **That an environment variable overrides the file.** This is Spring Boot's documented ordering, OS
  environment variables ranking above config-data imports, and it is what the README and the
  `application.yaml` comment state, but the experiment that would have proved it here was interrupted
  before it ran. It is worth one check before relying on it in a deployment.

Found while doing this, and worth knowing for a demonstration:

- **H2 in file mode allows one JVM at a time.** A second instance against the same `./data` fails at
  start-up with `Database may be already in use ... The file is locked`. This is H2 behaviour, not a
  defect of this project, and it surfaced because a process left over from this session was still
  holding the file. It is now documented in the README, with the two ways out: stop the first instance,
  or point the second at `jdbc:h2:mem:scratch`.
