# AI Usage Disclosure

## Tools Used

- **Claude Code** (Anthropic) — AI coding assistant used as a development accelerator

---

## How I Used It

I used Claude Code the way a senior developer uses a capable IDE or a fast-typing pair programmer: I drove every decision, and it translated those decisions into code.

My workflow throughout this project was:

1. **Design first** — I decided the architecture, package structure, API contract, and implementation approach before any code was written
2. **Dictate to Claude** — I described exactly what to implement and how, including constraints and non-obvious reasoning
3. **Review every output** — I read each generated file, caught issues, and corrected them
4. **Iterate deliberately** — when something was wrong or didn't match my intent, I identified the root cause and directed the fix

Claude Code did not make architectural decisions. It executed mine.

---

## Key Design Decisions I Made

These are decisions where I knew what I wanted and directed the implementation:

**Architecture**
- Layered hexagonal design: controller → service → repository, with domain objects decoupled from JPA entities and mappers at the persistence boundary
- Controller is pure HTTP — no business logic, no `PageRequest` construction (that belongs in the service layer)
- Separate ingestion service (`ExchangeRateIngestionService`) with two distinct paths: bulk load on empty DB, incremental daily sync via scheduler

**Data & Storage**
- Bundesbank SDMX REST API as the upstream data source — I evaluated the API response format (CSV over SDMX-ML) and chose the simpler parse path
- File-based H2 persistence so historical data survives restarts locally without re-fetching years of rates on every run
- `java.util.Currency` for ISO 4217 display names — I knew this was in the JDK and there was no need to call an external API or store names in the database

**Performance**
- Bulk load optimisation: local `HashMap` currency cache within the ingestion loop to avoid repeated DB round-trips, combined with `saveAll()` and Hibernate batch insert (`batch_size: 500`) — this reduced ~570k DB operations to ~61
- Caffeine cache on `getAvailableCurrencies()` and `getRateForCurrencyOnDate()` since these are read-heavy and change infrequently

**API Design**
- `GET /api/exchange-rates/{currency}/{date}` returns the most recent rate *on or before* the requested date — intentional fallback for weekends and public holidays
- Paginated `/api/exchange-rates` with a custom `PagedResponse<T>` record that exposes the pagination metadata a frontend needs (`totalPages`, `first`, `last`, etc.)
- Currency code normalisation to uppercase in the service layer so `usd` and `USD` behave identically
- `convertToEur` with zero-amount short-circuit (skip division, return zero) and rejection of negative amounts with a typed `InvalidAmountException` → 400

**Testing**
- TDD throughout — tests written before implementation, red → green → refactor
- `@WebMvcTest` slices for controller tests (fast, focused on HTTP layer)
- Pure unit tests with Mockito for service and ingestion logic
- `@SpringBootTest` + `@AutoConfigureTestDatabase` for caching integration test — I identified that the file-based H2 datasource would bleed state across test runs and directed the fix

---

## Where I Overrode or Rejected AI Suggestions

**OpenAPI controller annotations** — Claude Code generated `@Operation` and `@ApiResponse` annotations inline on each controller method. I rejected this: it clutters the controller and mixes documentation concerns into the HTTP layer. I also evaluated a `CurrencyApi` interface approach as an alternative and rejected that too — over-engineering for a service this size. The final choice (an `OpenApiConfig` bean with service-level `Info` metadata, no method-level annotations) was mine.

**`@RestControllerAdvice` response type** — the initial `GlobalExceptionHandler` returned `ResponseEntity<ProblemDetail>`. I changed it to return `ProblemDetail` directly, which Spring Boot 3 handles correctly and removes the unnecessary wrapping.

**Controller `Pageable` parameter** — the first pagination implementation had the controller construct a `PageRequest` and pass `Pageable` to the service. I flagged this as a layering violation (presentation layer creating application-layer objects) and directed the refactor: controller passes raw `int page, int size`, service builds the `PageRequest`.

---

## What I Would Have Built Without AI

The architecture, package layout, naming, testing strategy, and API design in this service are consistent with interview projects I built before AI coding tools existed — the same layered structure, thin controllers, constructor injection, `@RestControllerAdvice`, domain/entity separation, and mapper pattern appear across those prior projects verbatim.

What Claude Code changed was *throughput*, not *authorship*. A task that would have taken me a full day of typing and context-switching took a few focused hours. The code that exists in this repository reflects my decisions — Claude Code was the instrument, not the composer.
