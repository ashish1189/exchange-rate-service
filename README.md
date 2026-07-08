# 💱 Exchange Rate Service

A Spring Boot microservice that exposes EUR foreign exchange rates sourced from the **Deutsche Bundesbank** (German Central Bank). Supports historical rate lookup, currency listing, and amount conversion to EUR.

---

## 📡 Data Source

Rates are fetched from the [Bundesbank SDMX REST API](https://api.statistiken.bundesbank.de/rest/data/BBEX3/D..EUR.BB.AC.000) — a public endpoint providing daily EUR/FX rates for all available currencies going back decades.

- On first startup the service bulk-loads the entire historical dataset into a local H2 database
- A scheduler runs daily at **18:00 Mon–Fri** to fetch the latest rates
- Rates are expressed as: **1 EUR = X [currency]**

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Maven | 3.x |

### Run

```bash
mvn spring-boot:run
```

The service starts on `http://localhost:8080`.

> **Note:** On first run, historical data is bulk-loaded from the Bundesbank API. This may take a few seconds. Subsequent startups skip the bulk load since data is persisted locally in `./data/`.

### H2 Console

Browse the local database at `http://localhost:8080/h2-console`

| Field | Value |
|-------|-------|
| JDBC URL | `jdbc:h2:file:./data/exchangerates` |
| Username | `sa` |
| Password | *(empty)* |

### API Docs (Swagger UI)

Available at `http://localhost:8080/swagger-ui.html` once the service is running.

---

## 🔌 API Endpoints

All endpoints are prefixed with `/api`.

### `GET /api/currencies`

Returns all currencies for which exchange rate data is available.

**Response**
```json
[
  { "currencyCode": "AUD", "currencyName": "Australian Dollar" },
  { "currencyCode": "GBP", "currencyName": "British Pound" },
  { "currencyCode": "USD", "currencyName": "US Dollar" }
]
```

---

### `GET /api/exchange-rates`

Returns all historical EUR/FX rates, paginated and ordered by currency then date.

| Query Param | Type | Default | Description |
|-------------|------|---------|-------------|
| `page` | int | `0` | Page number (0-indexed) |
| `size` | int | `25` | Page size |

**Response**
```json
{
  "content": [
    { "currency": "AUD", "date": "2024-01-02", "rate": 1.6245 }
  ],
  "page": 0,
  "pageSize": 25,
  "totalElements": 152340,
  "totalPages": 6094,
  "first": true,
  "last": false
}
```

---

### `GET /api/exchange-rates/{currency}/{date}`

Returns the EUR/FX rate for a given currency on a given date. If no rate exists for that exact date (e.g. weekend or public holiday), the **most recent rate on or before** that date is returned.

| Path Param | Format | Example |
|------------|--------|---------|
| `currency` | ISO 4217 (case-insensitive) | `USD`, `usd` |
| `date` | `YYYY-MM-DD` | `2024-01-15` |

**Response**
```json
{ "currency": "USD", "date": "2024-01-12", "rate": 1.0982 }
```

---

### `GET /api/exchange-rates/convert`

Converts a foreign currency amount to EUR using the rate on or before the given date.

| Query Param | Type | Description |
|-------------|------|-------------|
| `currency` | string | ISO 4217 code (case-insensitive) |
| `amount` | decimal | Amount to convert (must be ≥ 0) |
| `date` | `YYYY-MM-DD` | Date for the rate |

**Response**
```json
{
  "currency": "USD",
  "amount": 100.00,
  "requestedDate": "2024-01-13",
  "rateDate": "2024-01-12",
  "rate": 1.0982,
  "convertedAmountInEur": 91.059900
}
```

> `rateDate` may differ from `requestedDate` when no rate exists on the requested date (weekends, holidays).

---

## ⚠️ Error Responses

Errors follow [RFC 9457 Problem Detail](https://www.rfc-editor.org/rfc/rfc9457) format.

| HTTP Status | Scenario |
|-------------|----------|
| `400 Bad Request` | Invalid date format, missing required param, or negative amount |
| `404 Not Found` | No rate exists on or before the requested date |

---

## 🏗️ Architecture & Design

```
┌─────────────────────────────────────────────┐
│               CurrencyController             │  ← REST layer
├─────────────────────────────────────────────┤
│              ExchangeRateService             │  ← Business logic + Caffeine cache
├────────────────────┬────────────────────────┤
│  ExchangeRateRepo  │    CurrencyRepository  │  ← Spring Data JPA
├────────────────────┴────────────────────────┤
│                H2 Database                   │  ← File-based persistence
└─────────────────────────────────────────────┘
         ▲
         │ startup + daily scheduler
┌────────┴──────────────┐
│  ExchangeRateIngestion │  ← Bundesbank API client
│       Service          │
└───────────────────────┘
```

The service follows **hexagonal architecture** principles — the controller handles only HTTP concerns and delegates everything to the service layer. Domain objects are kept separate from JPA entities, with mappers at the boundary.

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.2.2 |
| Language | Java 21 |
| Persistence | Spring Data JPA + H2 (file-based) |
| Caching | Caffeine (via Spring Cache) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| HTTP Client | Spring `RestClient` |
| Build | Maven |
| Testing | JUnit 5 + Mockito + AssertJ |

---

## 🧪 Testing Approach

The service was built using **Test-Driven Development (TDD)** throughout — tests were written before implementation, following the red → green → refactor cycle.

| Test type | Tool | What's covered |
|-----------|------|----------------|
| Controller (slice) | `@WebMvcTest` + MockMvc | HTTP layer, status codes, JSON shape |
| Service (unit) | JUnit 5 + Mockito | Business logic, edge cases, error paths |
| Ingestion (unit) | JUnit 5 + Mockito | Bulk load, daily sync, deduplication |
| Caching (integration) | `@SpringBootTest` + `@SpyBean` | Cache hit/miss behaviour |
| Context load | `@SpringBootTest` | Wiring sanity check |

Run all tests:

```bash
mvn test
```

> All `@SpringBootTest` tests use `@AutoConfigureTestDatabase` to replace the file-based H2 datasource with a fresh in-memory instance, keeping tests fully isolated.

---

## 📁 Project Structure

```
src/
├── main/java/.../
│   ├── currency/
│   │   ├── api/           # Controller, GlobalExceptionHandler, DTOs
│   │   ├── client/        # Bundesbank HTTP client + CSV parser
│   │   ├── config/        # OpenAPI config
│   │   ├── domain/        # Domain models (ExchangeRate, Currency)
│   │   ├── exception/     # ExchangeRateNotFoundException, InvalidAmountException
│   │   ├── repository/    # Spring Data JPA repos + JPA entities + mappers
│   │   └── service/       # ExchangeRateService, ExchangeRateIngestionService
│   └── CmCodingChallengeApplication.java
└── main/resources/
    └── application.yml
```

---

## 📄 Assignment

See [REQUIREMENTS.md](REQUIREMENTS.md) for the original assignment brief.
