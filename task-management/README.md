# Task Management API

A REST API for task management: user registration, authentication with a JWT, and a CRUD of each
user's own tasks. A user only ever sees and changes their own tasks.

Java 25 · Spring Boot 4.1.1 · Maven · Clean Architecture in four modules · H2 · Flyway · JWT (HS256).

## How this was built (GenAI exercise)

Written by an AI agent (Claude Code) from a written prompt. The code in this folder is the output.

| Step | Where |
|---|---|
| **Prompt.** The API described in one go (stack, modules, domain rules, contract, edge cases, tests), then turned into a meta-prompt with phases and a mandatory stop for approval | `docs/starter-prompt.md`, `docs/meta-prompt.md` |
| **Corrections.** The agent stopped with nine open questions. Four of the answers differed from its proposal (password limit in UTF-8 bytes, trimmed `description`, the rejected read-only field named in the error, different seed data) | `docs/open-decisions.md` |
| **Validation.** Full build (181 tests), a 39-call `curl` session against the running jar, review against the requirements, deviations, and what was *not* verified | `docs/validation-report.md` |
| **Edge cases, auth and validation.** Another user's task answers 404, `dueDate` in the past is rejected, unknown status, read-only fields, missing, invalid or expired token | [Rules](#rules), [Errors](#errors), `docs/validation-report.md` |

`domain` and `application` were written test first. No test was deleted, disabled or weakened.

## Modules

```
task-management          parent POM
├── domain               Task, TaskStatus, User, the invariants and the business exceptions. Plain Java.
├── application          use cases (input ports + services) and output ports. Plain Java.
├── infrastructure       JPA persistence, Flyway migrations, BCrypt, JWT issuing, the system clock.
└── web                  controllers, DTOs, validation, security, error handling. The executable module.
```

`domain` and `application` have no framework on their classpath, so an illegal import does not compile.

## Setup

Requirement: JDK 25. From the project root:

```bash
./mvnw clean verify                  # build and the whole test suite (181 tests)
./mvnw -pl web spring-boot:run       # run on http://localhost:8080
```

- The JWT key is read from the environment variable `JWT_SECRET` or, if not set, from `vault/JWT_SECRET`, a demo key committed so the app starts with no setup (at least 32 bytes).
- H2 in file mode at `./data/`. Flyway owns the schema and the demonstration data; deleting `data/` resets everything to the seed.

## Demonstration credentials

Flyway seeds one user and five tasks, with due dates relative to today (one is always overdue).

| Email | Password |
|---|---|
| `demo@example.com` | `demo1234` |

## Routes

Only the two authentication routes are public; every other route needs `Authorization: Bearer <accessToken>`.

| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| POST | `/api/v1/auth/register` | `name`, `email`, `password` | `201` + `{id, name, email}` | `400`, `409` |
| POST | `/api/v1/auth/login` | `email`, `password` | `200` + `{accessToken}` | `400`, `401` |
| POST | `/api/v1/tasks` | `title`, `description?`, `status?`, `dueDate` | `201` + task + `Location` | `400`, `401` |
| GET | `/api/v1/tasks?page&size&status` | — | `200` + page of tasks | `400`, `401` |
| GET | `/api/v1/tasks/{id}` | — | `200` + task | `400`, `401`, `404` |
| PUT | `/api/v1/tasks/{id}` | `title`, `description?`, `status?`, `dueDate` | `200` + task | `400`, `401`, `404` |
| DELETE | `/api/v1/tasks/{id}` | — | `204` | `400`, `401`, `404` |

A task is `{ id, title, description, status, dueDate, createdAt, updatedAt }`. The owner always comes from the token. The list is ordered by `dueDate`, then `id`; `size` goes from 1 to 100.

### Rules

- `title` is required, trimmed, 1 to 120 characters. `description` is optional, trimmed, up to 2000 characters.
- `status` is `TODO`, `IN_PROGRESS` or `DONE` (default `TODO`).
- `dueDate` may not be in the past on creation. On an update it is only validated when it changes, so an overdue task can still be closed. "Today" is the server's UTC date.
- A password is 8 characters to 72 UTF-8 bytes (the part BCrypt considers). Emails are compared case-insensitively.

### Errors

Every error is an RFC 9457 `ProblemDetail` with a fixed English `detail`, never a stack trace.

| Case | Status | `detail` |
|---|---|---|
| A field is invalid, out of range, or not a known status | `400` | `Validation failed` + `errors[]` |
| `id`, `ownerId`, `createdAt` or `updatedAt` sent in the body | `400` | `Validation failed`, `must not be sent` |
| Malformed JSON, an empty body, a date that is not a date | `400` | `Malformed request body` |
| Any login failure | `401` | `Invalid email or password` |
| Missing, invalid or expired token | `401` | `Authentication required` |
| A task that does not exist, **or belongs to another user** | `404` | `Task not found` |
| The email is already registered | `409` | `Email already registered` |
