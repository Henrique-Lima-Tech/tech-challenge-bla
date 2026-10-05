# Task Management API

A REST API for task management: user registration, authentication with a JWT, and a CRUD of each
user's own tasks. A user only ever sees and changes their own tasks.

Java 25 · Spring Boot 4.1.1 · Maven · Clean Architecture in four modules · H2 · Flyway · JWT (HS256).

## How this was built

The project was written by an AI agent (Claude Code), driven by a prompt rather than by ad-hoc
instructions. The point of the strategy is that every decision is written down before any code
exists, so the result can be audited against something: a contract, a numbered rule, an answered
question. Each step left its artifact in `docs/`, and they are all part of the delivery.

**1. The context first, in a starter prompt.** `docs/starter-prompt.md` describes the whole API in
one go — the product, the stack, the four modules and the dependency rule, the domain rules, the API
contract, the edge cases, the conventions and the test strategy — and asks for that description to be
turned into a meta-prompt. Nothing was improvised in a chat message later.

**2. The meta-prompt.** `docs/meta-prompt.md` is the result: the role, the context, the instruction,
few-shot examples of the expected shape, the constraints of what must *not* be done, and the output
format, split into three phases with a mandatory stop for approval. This is the document the agent
actually followed.

**3. Planning, and the decisions the AI handed back.** Running the meta-prompt produced
`docs/plan.md`: the API contract route by route, 40 numbered rules (`R-001`…`R-040`) each linked to
the requirement it comes from, 19 technical decisions (`D-01`…`D-19`) each with its reason, and the
implementation tasks ordered from the inside out. The agent did not decide everything on its own: it
stopped with nine open questions — things the requirements left ambiguous or contradictory, such as
whether a password limit of 72 means characters or UTF-8 bytes.

**4. The answers, on the record.** Those nine questions were collected in `docs/open-decisions.md`
and answered by the developer. **Four of the nine answers differed from the AI's proposal** and
changed rules, decisions and the scope of tasks — the password became a byte ceiling, `description`
became trimmed, a rejected read-only field had to name the field, and the seed data changed. The
answers were then folded back into `plan.md`, section 4, so the plan and the code never disagree.
Nothing ambiguous was resolved silently in the code.

**5. Implementation, one task at a time.** Fourteen tasks, in plan order, `domain` and `application`
test first: the test runs and fails, the minimum is implemented, the test runs and passes. No test
was ever deleted, disabled or weakened.

**6. Validation, written down honestly.** `docs/validation-report.md` records what was verified and
how: the full build, a 39-call `curl` session against the running application (kept verbatim in
`docs/curl-session.txt`), the code review against the requirements, every deviation from the approved
plan, and — explicitly — what was *not* verified. The report is meant to be read as evidence, not as
a claim.

| Artifact | What it holds |
|---|---|
| `docs/starter-prompt.md` | the original description of the API, written before anything else |
| `docs/meta-prompt.md` | the prompt the agent followed, with its phases and constraints |
| `docs/plan.md` | API contract, 40 rules, 19 decisions, the resolved questions, the task list |
| `docs/open-decisions.md` | the nine questions the AI raised, with the developer's answers |
| `docs/validation-report.md` | what was verified, the deviations, and what was not verified |
| `docs/curl-session.txt` | the 39 requests and responses, verbatim |

## Modules

```
task-management          parent POM
├── domain               Task, TaskStatus, User, the invariants and the business exceptions. Plain Java.
├── application          use cases (input ports + services) and output ports. Plain Java.
├── infrastructure       JPA persistence, Flyway migrations, BCrypt, JWT issuing, the system clock.
└── web                  controllers, DTOs, validation, security, error handling. The executable module.
```

The dependency rule is enforced by the module POMs alone: `web` depends on `application` and
`infrastructure`, `infrastructure` on `application`, `application` on `domain`. An illegal import in
`domain` or `application` does not compile, because the framework is not on their classpath.

## Setup

Requirements: JDK 25 and nothing else. Maven comes with the wrapper, and the demonstration key ships
with the project, so there is no setup step:

```bash
./mvnw clean verify                  # build and the whole test suite
./mvnw -pl web spring-boot:run       # run on http://localhost:8080
```

Or run the packaged jar, which is where the demonstration below comes from:

```bash
./mvnw clean package
java -jar web/target/web-0.0.1-SNAPSHOT.jar                      # port 8080
java -jar web/target/web-0.0.1-SNAPSHOT.jar --server.port=8081   # another port
```

Run both from the project root, since `./vault` and `./data` are resolved against the working
directory of the process. `spring-boot:run` is already configured to use the project root.

### The signing key and the vault

The JWT signing key has no default in the code, so the application cannot start without one. It is
read from `vault/`, a simulated secret store: every file in that folder is one property, the file name
being the key and the content being the value, the way Vault or Kubernetes mount secrets
(`spring.config.import: optional:configtree:./vault/`).

```
vault/JWT_SECRET      demo-only-jwt-secret-change-me-before-any-real-use
```

That value is committed on purpose: it exists so the demonstration starts with no setup, it is good
for a local demo only, and it is not a secret. A real deployment overrides it with an environment
variable, which ranks above the file:

```bash
export JWT_SECRET='a-secret-of-at-least-32-bytes-from-your-own-store'
```

The key must be at least 32 bytes, because HS256 requires 256 bits; a shorter one fails the start-up
with `security.jwt.secret must be at least 32 bytes`.

### The database

H2 in file mode at `./data/task-management.mv.db`, ignored by git. Flyway owns the schema and the
demonstration data; Hibernate only validates that the entities match the schema. Deleting the `data`
folder resets everything to the seed.

H2 in file mode allows **one JVM at a time**: a second instance fails with
`Database may be already in use ... The file is locked`. Stop the first one, or point the second at
another database with `--spring.datasource.url=jdbc:h2:mem:scratch`. The test suite is unaffected,
because it uses H2 in memory.

Overridable settings: `JWT_SECRET`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Demonstration credentials

Flyway seeds one user and five tasks belonging to that user. The due dates are relative to the current
date, so one task is always overdue and the others are always in the future.

| Email | Password |
|---|---|
| `demo@example.com` | `demo1234` |

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@example.com","password":"demo1234"}'
```

## Routes

Everything lives under `/api/v1`. Only the two authentication routes are public; every other route
needs `Authorization: Bearer <accessToken>`.

| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| POST | `/api/v1/auth/register` | `name`, `email`, `password` | `201` + `{id, name, email}` | `400`, `409` |
| POST | `/api/v1/auth/login` | `email`, `password` | `200` + `{accessToken}` | `400`, `401` |
| POST | `/api/v1/tasks` | `title`, `description?`, `status?`, `dueDate` | `201` + task + `Location` | `400`, `401` |
| GET | `/api/v1/tasks` | — | `200` + page of tasks | `400`, `401` |
| GET | `/api/v1/tasks/{id}` | — | `200` + task | `400`, `401`, `404` |
| PUT | `/api/v1/tasks/{id}` | `title`, `description?`, `status?`, `dueDate` | `200` + task | `400`, `401`, `404` |
| DELETE | `/api/v1/tasks/{id}` | — | `204`, empty body | `400`, `401`, `404` |

### Task

```json
{
  "id": 6,
  "title": "Demonstrate the API",
  "description": "Phase 3 curl session",
  "status": "TODO",
  "dueDate": "2026-12-31",
  "createdAt": "2026-10-05T18:09:01.177351Z",
  "updatedAt": "2026-10-05T18:09:01.177351Z"
}
```

`createdAt` and `updatedAt` are set by the server and are read-only. The owner always comes from the
token, so it is never in a request and never in a response.

### Rules

- `title` is required, trimmed, 1 to 120 characters.
- `description` is optional, trimmed, up to 2000 characters; a blank description is stored as `null`.
- `status` is `TODO`, `IN_PROGRESS` or `DONE`, defaults to `TODO` when not sent, and moves freely
  between the three values.
- `dueDate` is required, ISO (`2026-12-31`), and may not be in the past on creation. On an update it is
  only validated when it changes, so an already overdue task can still be edited and closed.
- "Today" is the UTC date of the server's clock.
- A password is at least 8 characters and at most 72 UTF-8 bytes, the part BCrypt considers.
- Emails are compared case-insensitively and without surrounding whitespace.

### Listing

`GET /api/v1/tasks` takes `page` (from 0, default 0), `size` (1 to 100, default 20) and an optional
`status`. Tasks are ordered by `dueDate` ascending, then by `id`. The response is always the page
shape, and a page past the end answers `200` with empty content and the real totals:

```json
{ "content": [], "page": 9, "size": 20, "totalElements": 6, "totalPages": 1 }
```

### Errors

Every error is an RFC 9457 `ProblemDetail` from a single handler, with a fixed English `detail`. No
response carries a stack trace, a database message or the rejected value.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "instance": "/api/v1/tasks",
  "detail": "Validation failed",
  "errors": [ { "field": "dueDate", "message": "must not be in the past" } ]
}
```

| Case | Status | `detail` |
|---|---|---|
| A field is invalid, out of range, or not a known status | `400` | `Validation failed` + `errors[]` |
| `id`, `ownerId`, `createdAt` or `updatedAt` sent in the body | `400` | `Validation failed`, `must not be sent` |
| Malformed JSON, an empty body, a date that is not a date | `400` | `Malformed request body` |
| Any login failure | `401` | `Invalid email or password` |
| Missing, invalid or expired token | `401` | `Authentication required` |
| A task that does not exist, **or belongs to another user** | `404` | `Task not found` |
| The email is already registered | `409` | `Email already registered` |

A task owned by someone else answers `404`, never `403`, so no response reveals that it exists.

## Tests

```bash
./mvnw clean verify                       # everything: 181 tests
./mvnw -pl domain test                    # domain only
./mvnw -pl application -am test           # use cases
./mvnw -pl infrastructure -am test        # persistence, security, clock
./mvnw -pl web test                       # controllers and the integration tests
```

| Module | Tests | What they cover |
|---|---|---|
| `domain` | 54 | every `Task` and `User` invariant, with plain JUnit and AssertJ |
| `application` | 27 | the seven use cases, with Mockito on the ports |
| `infrastructure` | 30 | `@DataJpaTest` on H2 in memory, BCrypt, JWT issuing, the clock |
| `web` | 70 | `@WebMvcTest` for status, JSON, validation and security, plus `@SpringBootTest` flows |

`domain` and `application` were written test first. Tests use H2 in memory and never load the
demonstration data, so each test owns the rows it asserts on.

## Out of scope, on purpose

No OpenAPI documentation, no architecture tests, no coverage threshold, no CI, no roles, no refresh
token, no logout, no password recovery.
