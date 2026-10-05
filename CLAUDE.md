# Tech Challenge: Pokémon API

A REST API in Java + Spring Boot that consumes the PokéAPI, replicates Pokémon into a local database with proprietary fields, and has authentication. These rules and the docs in `pokemon-api/docs/` cover **the backend of the `pokemon-api/` project only**; its frontend in `pokemon-api/frontend/` has its own docs in `pokemon-api/frontend/docs/`. **`task-management/` is a separate project with its own docs: these rules do not apply to it.** Paths written inside `pokemon-api/docs/` are relative to `pokemon-api/`. Deadline: 72h. One developer; **every stage is implemented by an AI agent** (you), one stage per fresh session, following `pokemon-api/docs/foundation-plan.md`.

## Sources of truth (read before acting, in this order)

1. `pokemon-api/docs/challenge/requirements.md`: the **closed list** of requirements, taken from the PDF. Anything not there is not a requirement.
2. `pokemon-api/docs/decisions.md`: approved technical decisions. Anything not there is not decided: ask.
3. `pokemon-api/docs/architecture.md`: modules, packages, dependency rules, testing per layer.
4. `pokemon-api/docs/api-contract.md`: routes, JSON and validation rules (approved).
5. `pokemon-api/docs/challenge/pokeapi.md`: the only PokéAPI endpoints and fields that may be used.
6. The feature spec in `pokemon-api/docs/specs/`.

## Stack (decided)

- **Language and framework:** Java 25, Spring Boot 4.1.1, Maven.
- **Modules:** the backend lives in `pokemon-api/backend/`, split into `domain`, `application`, `infrastructure`, `web`.
- **Database:** H2 in file mode for the application, H2 in memory for tests, Flyway for schema and seed data. No other database and no Testcontainers.
- **PokéAPI:** `RestClient`, with Spring Cache + Caffeine in front.
- **Security:** JWT with `spring-boot-starter-security-oauth2-resource-server`.
- **Errors:** `ProblemDetail`.
- **Mapping and boilerplate:** Lombok + MapStruct.

Commands (run inside `pokemon-api/backend/`):

- `./mvnw -q -pl <module> -am test`: tests for one module.
- `./mvnw -q verify`: full build.

## Non-negotiable rules

### Scope
- Implement **only** what has a `REQ-` in `requirements.md` or is in an approved spec.
- Do not invent requirements, rules, components, files, dependencies, fields or endpoints. If information is missing, **stop and ask**.
- Good ideas nobody asked for go to the spec's "Open questions" section, never into the code.
- Do not use anything listed as out of scope in `decisions.md`.
- Do not edit `requirements.md`, `decisions.md` or `api-contract.md` unless a human asks.

### Language
- **Everything in English:** classes, methods, variables, packages, tables, columns, JSON, log and error messages, commits, specs and documentation.
- Bean Validation messages are written explicitly in English (`@NotBlank(message = "must not be blank")`): the default messages follow the machine locale.

### Code
- Follow the dependency rule and the packages in `architecture.md`. Do not create new packages.
- `domain` and `application`: no Spring, JPA, Jackson, Bean Validation or MapStruct. Lombok is allowed.
- JPA entities and PokéAPI models never leave `infrastructure`.
- Records for DTOs, commands and results. Lombok for the rest of the boilerplate.
- Use `final` **everywhere** a value is not reassigned, in production and test code: fields, method and constructor parameters, local variables (`final var`) and `catch` variables. The only exceptions are listed in the "`final` everywhere" section of `pokemon-api/docs/architecture.md` (framework-injected test fields, record components, abstract interface method parameters, lambda parameters).
- Mappers are MapStruct interfaces, only in `infrastructure` and `web`.
- Adapters translate framework exceptions (`RestClientException`, `DataAccessException`, ...) into domain exceptions. Nothing from Spring reaches `application` or `domain`.
- Every new domain exception gets an `@ExceptionHandler` in `web/shared/error/GlobalExceptionHandler` that returns a `ProblemDetail` with a fixed English `detail`.
- **Logging in every stage**, as in the "Logging" section of `architecture.md`: `@Slf4j` only in `infrastructure` and `web`; `DEBUG` for the normal flow, `INFO` for startup facts, one `WARN` with context and cause where an external failure is handled; never secrets or bodies.
- Comments only for what the code cannot say (a framework quirk, a temporary behavior). No comments that narrate a requested change or restate the code.
- Prefer the simplest solution that passes the tests. No "future-proof" abstractions and no generic base classes.
- **No new dependency** in any `pom.xml` without a decision in `decisions.md`.
- Spring Boot 4 is recent. When unsure about an import, starter or annotation, check the "Spring Boot 4" section of `architecture.md` or ask. Do not guess from Boot 3.

### Tests (TDD)
- In `domain` and `application`, **test first**: run the test, show it fails, implement the minimum, run again.
- **Never** delete, disable (`@Disabled`) or weaken a test to make it pass. If a test looks wrong, stop and say so.
- No test calls the real PokéAPI. Use `pokemon-api/backend/infrastructure/src/test/resources/fixtures/pokeapi/`.
- Test names: `should<Result>When<Condition>`. Test packages mirror production packages.
- Test bodies use `// given`, `// when`, `// then` comments (`// when & then` when action and assertion are one expression). The naming and section patterns apply only to real test methods: helper methods, fixture loaders and the `contextLoads` smoke test do not follow them. In Spring tests, inject beans through an `@Autowired` constructor into `final` fields.

### Security
- No secrets in code: use environment variables.
- Passwords are stored only as a BCrypt hash, and are never returned or logged.
- 401 responses use a generic message.
- Never return a stack trace.
- Validate input format in `web` and invariants in `domain`.
- Every public route is listed with `permitAll()` in `web/shared/security/SecurityConfig`; everything else requires authentication.
- Never log passwords, tokens, `Authorization` headers, emails or request/response bodies.

## Definition of Done (per task)

- [ ] The module's tests pass; no test removed or disabled.
- [ ] Only the task's files changed; nothing outside the spec.
- [ ] Everything in English; no new dependency.
- [ ] Errors and status codes match the contract.
- [ ] Small commits: `test: ...` before `feat: ...` (or `refactor:` / `fix:` / `docs:`).

## Workflow

Each stage in `pokemon-api/docs/foundation-plan.md` is one agent session:

- `/spec <US>`: you write the spec; the developer approves it before any code.
- `/task <spec> <Tn>`: you implement one task with TDD, then stop and report.
- `/review`: the reviewer subagent checks the diff against these rules.
- You never commit without the developer saying "commit".
