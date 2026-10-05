# Backend architecture

> Based on D-03, D-04, D-05, D-07, D-09, D-12 to D-18 in `docs/decisions.md`. Code and documentation always in English.

## Modules and dependencies

```
web ──► application ──► domain
 │           ▲
 └──► infrastructure
```

| Module | Depends on | May use | Must not use |
|---|---|---|---|
| `domain` | nothing | Plain Java, Lombok | Spring, JPA, Jackson, Bean Validation, MapStruct, any other module |
| `application` | `domain` | Plain Java, Lombok | Spring, JPA, Jackson, MapStruct, `infrastructure`, `web` |
| `infrastructure` | `application`, `domain` | Spring, JPA, Flyway, `RestClient`, Caffeine, security, Lombok, MapStruct | `web` |
| `web` | all | Spring MVC, Bean Validation, Lombok, MapStruct | JPA entities and PokéAPI models (they **never leave** `infrastructure`) |

`web` is the only executable module: the application starts there.

## Packages

```
domain          com.tech.challenge.domain.<feature>.{model, valueobject, exception}
                com.tech.challenge.domain.shared.exception
application     com.tech.challenge.application.<feature>.{port.in, port.out, command, result, service}
                com.tech.challenge.application.shared.pagination
infrastructure  com.tech.challenge.infrastructure.pokemon.persistence.{adapter, entity, repository, mapper}
                com.tech.challenge.infrastructure.pokemon.pokeapi.{adapter, client, model, mapper, config}
                com.tech.challenge.infrastructure.user.persistence.{adapter, entity, repository, mapper}
                com.tech.challenge.infrastructure.user.security.{password, token}
                com.tech.challenge.infrastructure.shared.cache
                resources/db/{migration, seed}
web             com.tech.challenge.web.<feature>.{controller, dto.request, dto.response, mapper, config}
                com.tech.challenge.web.shared.{security, error, pagination}
```

`<feature>` is `pokemon` or `user`. These are the only packages allowed: **do not create new ones** without asking. A package folder is created together with its first class, never in advance with a `.gitkeep` (`valueobject` has no class yet, so it has no folder). The same goes for `resources/db/seed`: Flyway is already configured to read it and simply skips it while it does not exist, so the folder is created with the first seed script.

## Role of each piece

| Piece | Rule |
|---|---|
| `port.in` | Use case interface, called by `web`. One use case per interface. |
| `port.out` | Interface for what the core needs from outside (PokéAPI, repositories, password hashing, tokens). Implemented in `infrastructure`. Returns `Optional` when "not found" is a normal answer; the service turns an empty result into the domain's not-found exception. |
| `command` / `result` | Records for use case input and output. A read-only query may return the domain model directly instead of a field-by-field `result` copy (decided in US02, Q6). |
| `service` | Implements `port.in`. No framework annotations. |
| `web.<feature>.config` | Registers each `service` as a Spring bean (`@Bean`). |
| `mapper` | MapStruct interfaces (D-18), only in `infrastructure` and `web`. |
| `web.shared.security` | Public and protected route rules (D-07, D-11). |
| `web.shared.error` | Centralized error handling, `ProblemDetail` (D-17). |
| `db/migration`, `db/seed` | Flyway scripts (D-14). Never edit a script that was already applied; add a new one. |

## Testing per layer

| Layer | How | Test first? |
|---|---|---|
| `domain` | Plain JUnit | **Yes** |
| `application` | JUnit + mocks of the `port.out` interfaces | **Yes** |
| `infrastructure` / pokeapi | Recorded responses in `test/resources/fixtures/pokeapi`, no network | When cheap |
| `infrastructure` / persistence | H2 in memory, the same engine as the application (D-13, D-24) | When cheap |
| `web` | MVC slice test (status, JSON, validation, security) | May come with the code |

Test packages mirror production packages. Test names: `should<Result>When<Condition>`. Test bodies are split with `// given`, `// when`, `// then` comments; use `// when & then` when the action and the assertion are one expression (`assertThatThrownBy`, `mockMvc.perform(...).andExpect(...)`), and omit `// given` when there is no setup. These naming and section patterns apply only to real test methods. Helper methods (`expect(...)` in `PokeApiCatalogAdapterTest`, `map(...)` in `PokeApiDetailsMapperTest`, `PokeApiFixtures`) and the `ChallengeApplicationTests.contextLoads` smoke test do not follow them. Keep `contextLoads`: it is the only test that wires every module with the real configuration.

**`final` everywhere.** Every value that is not reassigned is `final`, in production **and** test code: fields, method and constructor parameters, local variables and `catch` variables. It documents intent, the compiler forbids accidental reassignment, and fields are safely published after construction.

| Where | How |
|---|---|
| Fields | `private final`; constructor injection with Lombok `@RequiredArgsConstructor` |
| Parameters | `final` on every method and constructor parameter, including annotated ones (`@PathVariable @NotBlank(...) final String idOrName`, `@Value("...") final String baseUrl`) and overridden framework methods |
| Local variables | `final var details = ...`; typed locals too (`final ProblemDetail problem = ...`) |
| `catch` | `catch (final RestClientException e)` |
| Spring tests (`@WebMvcTest`, `@RestClientTest`) | `@Autowired` constructor with `final` parameters setting `private final` fields (verified on Boot 4.1.1), as in `PokeApiCatalogAdapterTest` |

Exceptions, and only these:

| Exception | Why |
|---|---|
| `@Mock`, `@InjectMocks` (Mockito) and `@MockitoBean` (Spring) fields | The frameworks set them by reflection after the test instance is created |
| Record components | Already final by definition; Java does not accept the modifier there |
| Parameters of a record's compact constructor | They are reassigned on purpose to normalize values (`stats = List.copyOf(stats)`) |
| Parameters of abstract interface methods | `final` has no effect on a method without a body |
| Lambda parameters | They would need explicit types (`(final Stat stat) -> ...`); lambdas stay short |

## Spring Boot 4: confirmed pitfalls

> **Do not rely on the AI's memory for Boot 4.** This section holds **only** facts verified by building the project: imports, starter names, test annotations, Lombok/MapStruct setup on Java 25. For anything not listed here, check the official documentation or ask, and add what you verify.

Verified in S0 (FOUNDATION) with `./mvnw verify` on Java 25.0.4 and Boot 4.1.1:

- **Starter names that resolve and build:** `spring-boot-starter-webmvc` (not `-web`), `spring-boot-starter-restclient`, `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway` (`flyway-core` alone is not enough), `spring-boot-starter-cache`, `spring-boot-starter-validation`, `spring-boot-starter-security-oauth2-resource-server`.
- **The resource server starter was renamed.** `spring-boot-starter-oauth2-resource-server` (the name originally in D-07, now updated) still exists, but its 4.1.1 POM marks it deprecated in favor of `spring-boot-starter-security-oauth2-resource-server`. Use the new name.
- **Test support is split per technology:** `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-restclient-test`, `spring-boot-starter-security-oauth2-resource-server-test` (the last one includes `spring-boot-starter-test` and `spring-boot-starter-security-test`). They resolve and build; the slice annotations themselves are not exercised yet.
- **Versions managed by the Boot 4.1.1 BOM:** Lombok 1.18.46, H2 2.4.240, Caffeine 3.2.4, Flyway 12.4.0. **MapStruct is not managed:** the parent POM pins `mapstruct.version` 1.6.3 and `lombok-mapstruct-binding.version` 0.2.0.
- **Lombok + MapStruct on Java 25:** with explicit `annotationProcessorPaths` in the parent POM (Lombok, `mapstruct-processor`, `lombok-mapstruct-binding`, in that order), a `@Mapper(componentModel = "spring")` generated a `@Component` implementation that read a Lombok-generated getter. Lombok prints a `sun.misc.Unsafe` deprecation warning at compile time; it does not fail the build.
- **Context test:** `org.springframework.boot.test.context.SpringBootTest` (same import as Boot 3) starts the context from `web` with no Java configuration class. `@ActiveProfiles("test")` selects the H2 in-memory datasource from `application.yaml`.
- **Expected warnings:** Flyway logs that H2 2.4.240 is newer than its verified version (2.3.232), and "No migrations found" while `db/migration` and `db/seed` are empty. Neither fails the build.

Verified in S1 (US02) with `./mvnw verify` on Java 25.0.4, Boot 4.1.1, Spring Framework 7.0.9:

- **Test slice imports moved per technology:** `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` and `org.springframework.boot.restclient.test.autoconfigure.RestClientTest` (Boot 3: `org.springframework.boot.test.autoconfigure.web.servlet` / `.web.client`). `MockRestServiceServer` (`org.springframework.test.web.client`) and `MockMvc` keep their Spring Framework packages. Mock beans: `org.springframework.test.context.bean.override.mockito.MockitoBean` (`@MockBean` is no longer in `spring-boot-test` 4.1.1).
- **Slices in a non-executable module need a `@SpringBootConfiguration`.** `infrastructure` has none in production code, so its tests use `InfrastructureTestApplication` (`@SpringBootApplication`, in `infrastructure/src/test/java/com/tech/challenge/infrastructure`). `@WebMvcTest` in `web` finds `ChallengeApplication`.
- **What the web slice needs:** `@WebMvcTest(PokemonCatalogController.class)` works with `@Import({ PokemonDetailsResponseMapperImpl.class, SecurityConfig.class })` (the generated MapStruct class and the security chain are imported explicitly). `GlobalExceptionHandler` (`@RestControllerAdvice`) is picked up without an import. Spring Security filters run in the slice (a non-public route answered 403).
- **Jackson 3:** `RestClient` and MVC use `tools.jackson.core:jackson-databind` 3.1.5 (`tools.jackson.databind.json.JsonMapper`, unchecked `JacksonException`). The annotations stay in `com.fasterxml.jackson.annotation` (2.21). Jackson 3 **ignores unknown properties by default**; the PokéAPI models still declare `@JsonIgnoreProperties(ignoreUnknown = true)` to make the rule from `pokeapi.md` explicit.
- **`ProblemDetail.type` has no default in Spring Framework 7:** it stays `null` and is omitted from the JSON (RFC 9457 reads a missing type as `about:blank`). `GlobalExceptionHandler` sets `about:blank` explicitly, as the contract shows. `instance` is filled with the request path automatically.
- **HTTP client timeouts:** `spring.http.clients.connect-timeout` / `read-timeout` (Boot 4). `spring.http.client.*` is deprecated since 4.0.0. They apply to the auto-configured `RestClient.Builder`. Do not call `builder.requestFactory(...)` by hand: it replaces the `MockRestServiceServer` mock that `@RestClientTest` binds to the builder.
- **Method validation on path variables:** a constraint on a `@PathVariable` (no `@Validated` needed) raises `HandlerMethodValidationException`. Override `handleHandlerMethodValidationException` in the `ResponseEntityExceptionHandler` subclass to build the `errors[]` list. Write the constraint `message` in English (`@NotBlank(message = "must not be blank")`): the default Hibernate Validator messages follow the JVM/request locale.
- **Spring Security without an authentication mechanism:** with only `authorizeHttpRequests(...)` configured, a protected route answers **403**, not 401. Since AUTH (S3) `oauth2ResourceServer(...)` is configured and protected routes answer 401 (see the S3 facts below). Permit `/error`, or error dispatches of public routes are blocked too.

Verified in S2 (US01) with `./mvnw verify` on Java 25.0.4, Boot 4.1.1, Spring Framework 7.0.9:

- **Cache auto-configuration moved:** `org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration` (module `spring-boot-cache`; Boot 3: `org.springframework.boot.autoconfigure.cache`). It still needs `@EnableCaching` (here `infrastructure.shared.cache.CacheConfig`). `spring.cache.type: caffeine` + `spring.cache.caffeine.spec` give a `CaffeineCacheManager` that creates caches on first use with that spec.
- **`@RestClientTest` does not include the cache auto-configuration.** Add `@ImportAutoConfiguration(CacheAutoConfiguration.class)` and `@Import(CacheConfig.class)`. The test context (and its caches) is reused across test methods: clear every cache in `@BeforeEach`.
- **`@Cacheable` on a method returning `Optional`:** the value is cached unwrapped and `Optional.empty()` is cached as `null` (the Caffeine manager allows null values by default), so a PokéAPI 404 is cached too. Exceptions are never cached.
- **Parallel requests in `@RestClientTest`:** the default `MockRestServiceServer` expects requests in order. `MockServerRestClientCustomizer` is in `org.springframework.boot.restclient.test`, and the auto-configured one is `@ConditionalOnMissingBean`, so a `@TestConfiguration` bean `new MockServerRestClientCustomizer(UnorderedRequestExpectationManager::new)` replaces it (see `PokeApiCatalogAdapterSummariesTest`).
- **`requestMatchers(HttpMethod.GET, "/api/v1/pokemon/**")` also matches `/api/v1/pokemon`** (no trailing segment); covered by `PokemonCatalogControllerTest.shouldBePublicWhenListRequestHasNoToken`.
- **Query parameter validation:** `@Min`/`@Max` on `@RequestParam` raise `HandlerMethodValidationException` like path variables (no `@Validated`). A non-numeric value raises `MethodArgumentTypeMismatchException` before validation; override `handleTypeMismatch` in `ResponseEntityExceptionHandler` (`getPropertyName()` is the parameter name), otherwise the default `detail` echoes the rejected value.
- **MapStruct 1.6.3 maps generic records:** `PageResponse<PokemonSummaryResponse> toResponse(PageResult<PokemonSummary> page)` generates the element mapping with the concrete type arguments; no hand-written method needed.

Verified in S3 (AUTH) with `./mvnw verify` on Java 25.0.4, Boot 4.1.1, Spring Security 7.1.1, Hibernate 7:

- **JWT libraries come with the resource server starter:** `spring-security-oauth2-jose` (with `nimbus-jose-jwt` 10.9.1) and `spring-security-crypto` (BCrypt) are on the `infrastructure` and `web` classpaths through `spring-boot-starter-security-oauth2-resource-server`; no extra dependency. `NimbusJwtEncoder(new ImmutableSecret<>(secretKey))` + `JwsHeader.with(MacAlgorithm.HS256)` issues tokens; `NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()` validates signature, algorithm and `exp` (60 s clock skew). `oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))` picks up the `JwtDecoder` bean.
- **Request matchers:** `PathPatternRequestMatcher.withDefaults().matcher(...)` (`org.springframework.security.web.servlet.util.matcher`) combined with `OrRequestMatcher` works in `authorizeHttpRequests` and in custom code.
- **A bearer token on a public route is still validated by default:** an invalid or expired `Authorization: Bearer` header turns a `permitAll()` request into a 401. `SecurityConfig` sets a `bearerTokenResolver` that returns `null` for the public matchers (covered by `SecurityConfigTest.shouldIgnoreTokenWhenRouteIsPublic`; removing the resolver makes it fail).
- **401 as `ProblemDetail`:** the resource server's `authenticationEntryPoint` delegates to the `@Qualifier("handlerExceptionResolver") HandlerExceptionResolver`, so `GlobalExceptionHandler` (`@ExceptionHandler(AuthenticationException.class)`) builds it; `instance` is filled as for MVC errors. The default entry point would add `error_description` (why the token failed) to `WWW-Authenticate`. With no form login or HTTP Basic, this entry point also answers requests without a token, and Boot creates no generated-password `UserDetailsService`.
- **BCrypt 72-byte limit:** `BCryptPasswordEncoder.encode` throws `IllegalArgumentException` ("password cannot be more than 72 bytes"); `matches` does not throw for longer input. `@Size` counts characters, so a custom constraint (`MaxUtf8Bytes`) guards the bytes.
- **`@DataJpaTest`:** `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` (module `spring-boot-data-jpa-test`). It replaces the datasource with an embedded H2 and runs Flyway (`FlywayAutoConfiguration` is registered for `AutoConfigureDataSourceInitialization`) from the default `classpath:db/migration`. Each test is transactional and rolled back.
- **`@AutoConfigureMockMvc`:** `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`.
- **`ApplicationContextRunner`** (`org.springframework.boot.test.context.runner`) is available in `infrastructure` tests; `context.getStartupFailure()` checks fail-fast configuration.
- **`ddl-auto`:** with Flyway present Hibernate does not create tables; `spring.jpa.hibernate.ddl-auto: validate` makes a mismatch between entity and migration fail the context (and every `@DataJpaTest` that sets it).
- **Hibernate logs constraint violations with the values:** logger `org.hibernate.orm.jdbc.error` writes a `WARN` such as `Unique index or primary key violation: ... VALUES ( ... 'ash@example.com' )`. `application.yaml` sets it to `ERROR`. For the same reason, an adapter that translates a constraint violation logs its `WARN` without the cause.
- **Translating one constraint:** `DataIntegrityViolationException.getCause()` is Hibernate's `org.hibernate.exception.ConstraintViolationException`; on H2 its `getConstraintName()` contains the name from the migration (compared lowercase).
- **Test-only properties for every web test:** a file at `web/src/test/resources/config/application.yaml` is loaded **in addition to** the main `application.yaml` (location `classpath:/config/`, higher precedence), without a profile. A test `application.yaml` at the classpath root would instead hide the main one.
- **Secrets as files (`configtree`):** `spring.config.import: optional:configtree:./vault/` maps each file name to a property and its content to the value (trailing newline trimmed). Environment variables take precedence. The path is relative to the **working directory**: `java -jar` from `backend/` finds `backend/vault/`; `spring-boot:run` and the IDE run from `backend/web` and do not (with `optional:` they then fail on the unresolved `${JWT_SECRET}`). `optional:` lets tests, whose working directory is the module folder, start without it.
- **Jackson 3 in tests:** `JsonMapper.builder().build().writeValueAsString(map)` / `readTree(json).get("x").asString()` (`tools.jackson.databind.json.JsonMapper`).

Verified in S4 (US03) with `./mvnw verify` on Java 25.0.4, Boot 4.1.1, Spring Framework 7.0.9, Hibernate 7, H2 2.4.240:

- **`RestClient` encodes `/` inside a URI variable:** `uri("/pokemon/{idOrName}/", "../berry/1")` requests `/pokemon/..%2Fberry%2F1/`, so a body value cannot walk the PokéAPI path (covered by `PokeApiCatalogAdapterSummaryTest.shouldEncodeIdOrNameWhenItContainsAPathSeparator`).
- **List columns:** `@ElementCollection` + `@CollectionTable` + `@OrderColumn(name = "position")` + `@Column(name = ..., length = ...)` on a `List<String>` passes `ddl-auto: validate` against a Flyway table `(<owner>_id, position, <value>)`. `position` is not reserved in H2; `value` is, so do not use it as a column name. MapStruct copies the lists into `new ArrayList<>(...)` for the entity setters.
- **Concurrent inserts of the same unique key on H2:** the second transaction waits for the first one to commit, then fails with `DataIntegrityViolationException` whose cause is Hibernate's `ConstraintViolationException` with the constraint name, the same path as a sequential duplicate (covered by `LocalPokemonSyncIntegrationTest`).
- **Container element constraints:** `List<@NotBlank @Size(max = 30) String>` on a request record reports the field as `internalTags[1]` in `MethodArgumentNotValidException`; constraints on the list itself report `internalTags`.
- **`ServletUriComponentsBuilder.fromCurrentRequest()`** builds an absolute `Location` (`http://localhost/api/v1/local/pokemon/10` under MockMvc).

Verified in S5 (US04) with `./mvnw -q verify` on Java 25.0.4, Boot 4.1.1, Spring Framework 7.0.9, Hibernate 7, H2 2.4.240:

- **A constraint on one parameter makes Spring validate the whole method.** With `@PathVariable @Positive ... final long id` next to `@RequestBody @Valid`, the body violations no longer arrive as `MethodArgumentNotValidException`: they come inside `HandlerMethodValidationException` as a `ParameterErrors` result (`org.springframework.validation.method`), the only one that knows the field path (`name`, `internalTags[0]`). Handling just `getResolvableErrors()` reports every body error under the parameter name (`request`), so `GlobalExceptionHandler` checks for `ParameterErrors` first.
- **`@ElementCollection` is lazy, so a read that maps it needs a transaction.** `JpaRepository.findById` followed by MapStruct mapping outside a transaction throws `LazyInitializationException` ("no session"); `@Transactional(readOnly = true)` on the adapter method fixes it, and it must not rely on `spring.jpa.open-in-view`, which only exists during a web request. A `@DataJpaTest` method annotated `@Transactional(propagation = Propagation.NOT_SUPPORTED)` proves it (and commits, so it uses its own unique values).
- **`saveAndFlush` of a detached entity that already has an id merges it:** every column is updated and both `@ElementCollection` tables are rewritten, extra `position` rows deleted, so a full replacement (D-25) needs no explicit delete.
- **`@Digits(integer = 4, fraction = 1)` counts trailing zeros:** `6.00` is rejected (two fraction digits), while `BigDecimal.stripTrailingZeros().scale()` (the domain invariant) accepts it.
- **`@UniqueElements`** (`org.hibernate.validator.constraints`) is on the classpath through `spring-boot-starter-validation`, compares elements with `equals` and reports the error on the list field.
- **`MethodArgumentTypeMismatchException.getPropertyName()`** is the path variable name (`id`) for a non-numeric path segment, like the request parameters of US01.

## Logging (all stages)

**Why it matters.** Tests prove the code is right; logs show what the running application actually did. When a request misbehaves in the demo or in Docker, there is no debugger attached: the logs are the only way to see which external call was made, whether it failed, and which response the client got. They turn a vague "it returned 502" into "the species call for 'eevee' timed out". Good logs make problems faster to find and support the "functionality without errors" criterion of the panel.

**General rules** (developer request, 2026-10-03; apply to every stage):

| Rule | Detail |
|---|---|
| Language | English only, like all code (D-05) |
| Where | `infrastructure` and `web` only. `domain` and `application` do not log: they would need `slf4j-api`, a new dependency, and their exceptions already carry the information the edges log. |
| How | Lombok `@Slf4j`; parameterized messages (`log.debug("Fetching Pokemon '{}'", idOrName)`), never string concatenation; the exception goes as the **last** argument so the stack trace is printed |
| `DEBUG` | Normal flow worth following when debugging |
| `INFO` | Startup and configuration facts. Never one line per request. |
| `WARN` | A handled failure of an external dependency, logged **once**, where the context is known (the adapter), with the cause |
| `ERROR` | Unexpected failures only |
| Never log | Passwords, tokens, `Authorization` headers, emails, full request or response bodies |
| User input | It may appear in logs: Spring Security's `StrictHttpFirewall` rejects encoded CR/LF in the URL with 400 (covered by `SecurityConfigTest`), so a path value cannot forge log lines. Bodies are never logged. |

**What to log, per kind of component:**

| Component | Log | Example |
|---|---|---|
| Controller | `DEBUG` when a request arrives, with its identifiers (never the body) | `Received request for Pokemon details '{}'` |
| External HTTP client (PokéAPI) | `DEBUG` for each call and for an expected 404 | `Fetching species from PokeAPI: {}`, `PokeAPI answered 404 for Pokemon '{}'` |
| Adapter of an external system | `DEBUG` with the result built; one `WARN` with context and cause when it translates a failure | `PokeAPI request failed for Pokemon '{}'` |
| Persistence adapter (from US03) | `DEBUG` after create, update and delete, with the id; no line per read; `WARN` with the cause if a `DataAccessException` is translated, **except** a constraint violation, whose message contains the rejected values (AUTH, A9) | `Saved local Pokemon {} (pokeApiId {})` |
| Authentication (AUTH) | `DEBUG` for a registration or a successful login with the user id; a failed login at `DEBUG` without the email or password | `Login failed: invalid credentials` |
| `GlobalExceptionHandler` | `DEBUG` for every 4xx answered; `ERROR` with the stack trace for an unexpected 5xx, if a handler for it is ever added | `Answering 404: Pokemon '{}' not found` |
| Configuration (`@Configuration`) | `INFO` for external URLs or modes in use; never secrets | `PokeAPI base URL: {}` |

**Levels.** `application.yaml` sets `logging.level.com.tech.challenge: INFO`. To follow each request and external call, start with `--logging.level.com.tech.challenge=DEBUG` (or the environment variable `LOGGING_LEVEL_COM_TECH_CHALLENGE=DEBUG`).

## General conventions (from US02, apply to every stage)

| Convention | Detail |
|---|---|
| Exception translation | Adapters catch framework exceptions (`RestClientException`, `DataAccessException`, ...) and throw a domain exception (`ExternalServiceUnavailableException`, ...). Nothing from Spring reaches `application` or `domain`. |
| Not found | The `port.out` returns `Optional`; the service throws the domain's not-found exception; `web` answers 404. |
| External failures | A 404 from an external system on the **primary** resource means "not found" (404). Any other external failure (other status, timeout, unreadable body, a 404 on a secondary call) means 502. |
| PokéAPI lookups by name | Trim and lowercase the identifier in the application service before calling the port (the PokéAPI only knows lowercase names). Reuse this for `POST /api/v1/local/pokemon` (`"pokemon": "Pikachu"`). |
| Error responses | Every new domain exception gets an `@ExceptionHandler` in `GlobalExceptionHandler` built with its `problem(status, detail)` helper: `type` `about:blank`, fixed English `detail`, never a stack trace or an upstream body. Validation errors use `detail` "Validation failed" plus `errors[]` of `{field, message}`. |
| Validation messages | Written explicitly in English in every Bean Validation annotation (`@Size(max = 50, message = "size must be at most 50")`); the defaults follow the machine locale. |
| Public routes | Listed with `permitAll()` in `SecurityConfig`; everything else requires authentication. |
| External HTTP clients | Timeouts through `spring.http.clients.*`; never set a request factory on the `RestClient.Builder` by hand (it breaks `@RestClientTest`). Follow URLs returned by the PokéAPI with `uri(URI.create(url))`. |
| Comments | Only for what the code cannot say (a framework quirk, a temporary behavior). No comments that narrate a requested change or restate the code. |

## Reference patterns (from US02)

Later stages copy these classes. Paths are relative to each module's `com.tech.challenge.<layer>` package.

| Layer | Copy | Pattern |
|---|---|---|
| domain | `pokemon.model.PokemonDetails`, `EvolutionStage` | Record; invariants in the compact constructor (`IllegalArgumentException`); lists copied with `List.copyOf`, `null` → empty |
| domain | `pokemon.exception.PokemonNotFoundException`, `shared.exception.ExternalServiceUnavailableException` | Unchecked; fixed English message; mapped to HTTP only in `web` |
| application | `pokemon.port.in.GetPokemonDetailsUseCase`, `pokemon.port.out.PokemonCatalogPort`, `pokemon.service.GetPokemonDetailsService` | One use case per interface; the port returns `Optional` and the service decides "not found"; the service has no annotations except Lombok `@RequiredArgsConstructor` |
| application test | `GetPokemonDetailsServiceTest` | `@ExtendWith(MockitoExtension.class)`, `@Mock` port, `@InjectMocks` service; `// given` / `// when` / `// then` |
| infrastructure | `pokeapi.model.PokeApiPokemon` | Record with nested records; only documented fields; `@JsonProperty` for snake_case; `@JsonIgnoreProperties(ignoreUnknown = true)` |
| infrastructure | `pokeapi.client.PokeApiClient`, `pokeapi.config.PokeApiConfig` | One `RestClient` bean built from the Boot `RestClient.Builder` + `pokeapi.base-url`; 404 → `Optional.empty()` in the client; follow PokéAPI URLs with `uri(URI.create(url))` |
| infrastructure | `pokeapi.adapter.PokeApiCatalogAdapter` | Implements the port; wraps every `RestClientException` in `ExternalServiceUnavailableException` |
| infrastructure | `pokeapi.mapper.PokeApiDetailsMapper` | MapStruct, `componentModel = "spring"`, multi-source `@Mapping`, `@Named` default method for custom logic |
| infrastructure test | `PokeApiDetailsMapperTest`, `PokeApiCatalogAdapterTest`, `pokeapi.PokeApiFixtures` | Mapper: plain JUnit with `Mappers.getMapper`. Adapter: `@RestClientTest(properties = ...)` + `@Import` + `MockRestServiceServer` serving the recorded fixtures, injected through an `@Autowired` constructor into `final` fields; `// given` / `// when` / `// then` |
| web | `pokemon.controller.PokemonCatalogController`, `pokemon.dto.response.*`, `pokemon.mapper.PokemonDetailsResponseMapper` | Controller calls the use case and maps domain → record DTO with MapStruct; format validation with Bean Validation on parameters |
| web | `pokemon.config.PokemonUseCaseConfig` | `@Bean` per application service |
| web | `shared.error.GlobalExceptionHandler` | Extends `ResponseEntityExceptionHandler`; one `@ExceptionHandler` per domain exception returning `ProblemDetail` with `type` `about:blank`; validation → `detail` "Validation failed" + `errors[]` of `{field, message}` |
| web | `shared.security.SecurityConfig` | One stateless `SecurityFilterChain`; public routes listed with `permitAll()`; everything else authenticated |
| infrastructure / web | `PokeApiClient`, `PokeApiCatalogAdapter`, `GlobalExceptionHandler` | Logging as in the "Logging" section: `DEBUG` per external call, one `WARN` with context and cause in the adapter, `DEBUG` for 4xx answers |
| web test | `PokemonCatalogControllerTest` | `@WebMvcTest(Controller.class)` + `@Import({MapperImpl, SecurityConfig})` + `@MockitoBean` use case; `MockMvc` through an `@Autowired` constructor into a `final` field; `jsonPath` against the contract JSON; `// given` / `// when & then` |

## Reference patterns (from US01)

| Layer | Copy | Pattern |
|---|---|---|
| application | `shared.pagination.PageResult` | Page record with invariants; build it with `PageResult.of(content, page, size, totalElements)`, which computes `totalPages` |
| web | `shared.pagination.PageResponse`, `PokemonCatalogController.list` | Pagination: `@RequestParam(defaultValue = ...)` with `@Min`/`@Max` and explicit English messages (D-26); MapStruct maps `PageResult<X>` → `PageResponse<XResponse>` |
| infrastructure | `shared.cache.CacheConfig`, `@Cacheable` on `pokeapi.client.PokeApiClient` | Cache the raw external responses on the client (D-15), one cache per resource, names as constants in the client; bounds in `spring.cache.caffeine.spec` |
| infrastructure test | `pokeapi.client.PokeApiClientCacheTest` | Proving a cache: `@RestClientTest` + `@ImportAutoConfiguration(CacheAutoConfiguration.class)`, each request expected once, call twice, `server.verify()`; caches cleared in `@BeforeEach` |
| infrastructure | `pokeapi.adapter.PokeApiCatalogAdapter.fetchInParallel` / `limited` | Parallel external calls: virtual-thread executor in try-with-resources, one `CompletableFuture` per item joined in input order; the first failure interrupts the rest (`shutdownNow()`); every external call goes through the shared D-28 `Semaphore`, holding a permit for one call only |
| infrastructure test | `PokeApiCatalogAdapterSummariesTest`, `PokeApiCatalogAdapterConcurrencyTest` | Parallel adapter: unordered `MockRestServiceServer` for the HTTP behavior; plain Mockito client with blocking answers to measure calls in flight (cap, parallelism, fail-fast) without timing assumptions where possible |

## Reference patterns (from AUTH)

| Layer | Copy | Pattern |
|---|---|---|
| infrastructure | `resources/db/migration/V1__create_users_table.sql` | Flyway naming `V<n>__<snake_case_description>.sql`, next free number (US03 starts at `V2`; seed scripts in `db/seed` share the same version sequence). Plural snake_case table, `BIGINT GENERATED BY DEFAULT AS IDENTITY` primary key, named constraints (`uk_<table>_<column>`), column lengths from D-27. Never edit an applied script. |
| infrastructure | `user.persistence.entity.UserEntity` | `@Entity @Table(name = ...)`, Lombok `@Getter @Setter @NoArgsConstructor`, `@GeneratedValue(strategy = IDENTITY)`, `@Column` lengths matching the migration (checked by `ddl-auto: validate`) |
| infrastructure | `user.persistence.repository.UserJpaRepository` | Spring Data `JpaRepository<Entity, Long>` with derived queries; never used outside the adapter |
| infrastructure | `user.persistence.mapper.UserPersistenceMapper` | MapStruct `toDomain(entity)` (record constructor) / `toEntity(domain)` (setters) |
| infrastructure | `user.persistence.adapter.UserPersistenceAdapter` | Implements the `port.out`; `Optional` for lookups; `saveAndFlush` so constraint violations surface inside the adapter; only the meaningful `DataIntegrityViolationException` (checked by constraint name) becomes a domain exception, the rest is rethrown; `DEBUG` with the id after a write |
| infrastructure test | `UserPersistenceAdapterTest` | `@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")` + `@Import({Adapter, MapperImpl})`, adapter injected through an `@Autowired` constructor; runs the real Flyway migrations on H2 in memory |
| domain / application / web | `User`, `RegisterUserCommand`, `LoginCommand`, `AccessTokenResult`, `RegisterRequest`, `LoginRequest` | Records holding a password, hash or token override `toString` to leave it out |
| application | `RegisterUserService`, `LoginService` | Normalize identifiers (trim + lowercase email) in the service; one domain exception for every credential failure |
| infrastructure | `user.security.password.BCryptPasswordHasher` | `port.out` implemented with Spring Security's `BCryptPasswordEncoder` (default strength) |
| infrastructure | `user.security.token.JwtConfig`, `JwtTokenAdapter` | One `@Configuration` parses the HS256 key from `security.jwt.secret` (`${JWT_SECRET}`, from the environment or the simulated vault file `vault/JWT_SECRET`, ≥ 32 bytes, fail fast without printing it) and exposes `JwtEncoder` and `JwtDecoder`; the adapter issues `sub`/`iat`/`exp` |
| infrastructure test | `JwtTokenAdapterTest`, `JwtConfigTest` | Plain JUnit with `new JwtConfig(secret)` (encode, then decode with the real decoder); `ApplicationContextRunner` for startup failures |
| web | `user.controller.AuthController`, `user.dto.request.*` | `@RequestBody @Valid` records; text fields trimmed in the compact constructor (runs before validation), passwords never trimmed; explicit English messages; `@ResponseStatus(CREATED)` |
| web | `shared.security.SecurityConfig` | One stateless chain; `PUBLIC_ROUTES` (`OrRequestMatcher`) used by `permitAll()` **and** the bearer token resolver; `oauth2ResourceServer().jwt()`; entry point delegating to `HandlerExceptionResolver`; `@Import(JwtConfig.class)` so slice tests importing `SecurityConfig` get the decoder |
| web | `shared.error.GlobalExceptionHandler` | `handleMethodArgumentNotValid` → "Validation failed" + `errors[]`; `AuthenticationException` → 401 "Authentication required" + `WWW-Authenticate: Bearer` |
| web test | `SecurityConfigTest` | Security proofs: inject the `JwtEncoder` bean to mint valid and expired tokens; tamper with the signature; no token / malformed / bad signature / expired → 401 `ProblemDetail`; valid → past security (404 in a slice without the controller); stale token on a public route → 200. Protected endpoint tests from US03 on send `Authorization: Bearer <token>` minted the same way. |
| web test | `AuthFlowIntegrationTest` | `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` for one end-to-end flow; the in-memory database lives for the whole JVM, so each test uses its own data |

## Reference patterns (from US03)

| Layer | Copy | Pattern |
|---|---|---|
| domain | `pokemon.model.LocalPokemon` | Model reused by create and update: invariants for **every** D-27 limit in the compact constructor; lists of texts checked element by element **before** `List.copyOf` (it throws `NullPointerException` on a null element) |
| infrastructure | `resources/db/migration/V2__create_local_pokemon_tables.sql`, `pokemon.persistence.entity.LocalPokemonEntity` | A list field gets its own table with a composite PK (`<owner>_id`, `position`) and a named FK; `@ElementCollection` + `@OrderColumn` keeps the order |
| infrastructure | `pokemon.persistence.adapter.LocalPokemonPersistenceAdapter` | No lookup before the insert: the unique constraint is the only duplicate check, translated by name (AUTH pattern), so concurrent requests end in one success and one 409 |
| infrastructure test | `LocalPokemonPersistenceAdapterTranslationTest` | When domain invariants make other constraint violations impossible to provoke, a Mockito test with a mocked `JpaRepository` proves they are rethrown. Do not alter the schema inside a `@DataJpaTest`: DDL commits in H2 |
| web | `pokemon.dto.request.SyncPokemonRequest`, `UniqueIgnoringCase` | Trim every text, including list elements, in the compact constructor; element constraints with `List<@NotBlank ... String>`; case-insensitive uniqueness with a custom constraint (`@UniqueElements` compares with `equals`) |
| web | `pokemon.controller.LocalPokemonController` | `201` with `ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")...)`; the arrival `DEBUG` never logs a value read from the body (CR/LF in JSON strings are not rejected by the firewall) |
| web test | `LocalPokemonControllerTest`, `LocalPokemonSyncIntegrationTest` | Protected endpoints: every request carries a token minted with the `JwtEncoder` bean, plus one no-token 401 test. Concurrency proof: `@SpringBootTest` with the `port.out` as `@MockitoBean` whose answer waits on a `CyclicBarrier(2)`, two requests from an executor, then count the rows |

## Reference patterns (from US04)

| Layer | Copy | Pattern |
|---|---|---|
| domain | `pokemon.exception.LocalPokemonNotFoundException` | A missing **local record** is its own exception, apart from the catalog's `PokemonNotFoundException`; it carries the id for the handler's log |
| application | `pokemon.service.UpdateLocalPokemonService` | Full replacement (D-25): load by id through the port, keep the identifiers of the stored record, rebuild the domain model with the command values (so the invariants run), save |
| application | `pokemon.port.out.LocalPokemonRepositoryPort.findById` | A write port grows a `findById` returning `Optional`; `save` serves create **and** update (JPA merge), so no second method is needed |
| infrastructure | `pokemon.persistence.adapter.LocalPokemonPersistenceAdapter.findById` | A read that maps lazy `@ElementCollection` lists is `@Transactional(readOnly = true)` |
| web | `pokemon.dto.request.UpdateLocalPokemonRequest`, `HttpUrl` | Full-replacement request record: the identifiers are declared with `@Null` so sending them is a 400 (D-27) while other unknown properties stay ignored; one Bean Validation annotation per D-27 limit with an explicit English message; a custom constraint for the `http`/`https` URL format |
| web | `shared.error.GlobalExceptionHandler.fieldErrorsOf` | When a method mixes a constrained parameter with a `@Valid` body, map `ParameterErrors` to the field path so `errors[]` keeps the contract shape |
| web test | `LocalPokemonUpdateControllerTest` | Validation proof: one parameterized case per rule (field, body, expected message) built from a valid body with one field replaced or removed (`bodyWith` / `bodyWithout`), plus the general rules (malformed, empty, identifiers in the body, path id not numeric or ≤ 0) |
| web test | `LocalPokemonUpdateIntegrationTest` | Update end to end: create the row with the US03 sync (PokéAPI mocked), `PUT`, then check the columns and both list tables; absent optional fields become `NULL` and the tag table empty |
