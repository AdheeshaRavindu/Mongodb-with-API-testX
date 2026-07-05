# ConvertHub — Project Documentation

Complete reference for the **Mongodb-with-API-testX** repository: architecture, APIs, labs, setup, deployment, and troubleshooting.

---

## Table of contents

1. [Overview](#overview)
2. [Live URLs & repository](#live-urls--repository)
3. [Architecture](#architecture)
4. [Tech stack](#tech-stack)
5. [Project structure](#project-structure)
6. [Services & ports](#services--ports)
7. [MongoDB data model](#mongodb-data-model)
8. [Temperature API (port 8081)](#temperature-api-port-8081)
9. [Currency API (port 8082)](#currency-api-port-8082)
10. [Lab 04 — Safety check & filtered history](#lab-04--safety-check--filtered-history)
11. [Lab 05 — API key authentication](#lab-05--api-key-authentication)
12. [Frontend (web UI)](#frontend-web-ui)
13. [Docker setup](#docker-setup)
14. [Local development (without Docker)](#local-development-without-docker)
15. [Deployment](#deployment)
16. [Environment variables](#environment-variables)
17. [Conversion logic](#conversion-logic)
18. [Troubleshooting](#troubleshooting)
19. [Related files in `docs/`](#related-files-in-docs)

---

## Overview

**ConvertHub** is a full-stack microservices application for:

- **Currency conversion** — USD → LKR with fixed demo exchange rate and MongoDB history
- **Temperature conversion** — Celsius, Fahrenheit, and Kelvin with MongoDB history
- **Lab features** — safety-check endpoint, filtered history, and MongoDB-backed API key auth on temperature convert

The system uses two independent Spring Boot services, each with its own MongoDB database, plus a static HTML/CSS/JS frontend served by nginx (Docker) or Cloudflare Pages (production).

---

## Live URLs & repository

| Resource | URL |
|----------|-----|
| **Live demo (custom domain)** | [https://api.adheesha.dev](https://api.adheesha.dev) |
| **Cloudflare Pages** | [https://converthub-mongodb-api.pages.dev](https://converthub-mongodb-api.pages.dev) |
| **GitHub repository** | [https://github.com/AdheeshaRavindu/Mongodb-with-API-testX](https://github.com/AdheeshaRavindu/Mongodb-with-API-testX) |

### Production API backends (Cloudflare Workers)

When the frontend is **not** on `localhost`, it calls these Worker URLs:

| Service | URL |
|---------|-----|
| Currency | `https://currency-converter.vikumkodikara123.workers.dev/api/currency` |
| Temperature | `https://temperature-converter.vikumkodikara123.workers.dev/api/temperatures` |

When running **locally** (Docker or dev), the frontend uses `http://localhost:8081` and `http://localhost:8082` instead.

> **Note:** Lab 05 API key validation is implemented in the **Spring Boot** temperature service. The Cloudflare Workers may behave differently unless updated separately.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Browser (ConvertHub UI)                      │
│              localhost:3000  OR  api.adheesha.dev                │
└────────────────────────────┬────────────────────────────────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
     (local)  ▼                             ▼  (production)
   localhost:8081                   Cloudflare Workers
   localhost:8082                   (currency + temperature)
              │                             │
              ▼                             ▼
   ┌──────────────────┐          (external hosted APIs)
   │  tempconv        │
   │  Spring Boot     │
   │  port 8081       │
   └────────┬─────────┘
            │
            ▼
   ┌──────────────────┐
   │  MongoDB         │
   │  temp_db :27017  │
   │  collections:    │
   │  - conversions   │
   │  - api_keys      │
   └──────────────────┘

   ┌──────────────────┐
   │  currencyconvertor│
   │  Spring Boot     │
   │  port 8082       │
   └────────┬─────────┘
            │
            ▼
   ┌──────────────────┐
   │  MongoDB         │
   │  currency_db     │
   │  port 27018      │
   └──────────────────┘
```

---

## Tech stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot **4.0.5**, Java **21** |
| Database | MongoDB **7.0** |
| Frontend | HTML, CSS, vanilla JavaScript |
| Frontend fonts | Outfit (UI), IBM Plex Mono (numbers) |
| Containerization | Docker, Docker Compose |
| Production hosting | Cloudflare Pages (frontend) |
| Production APIs | Cloudflare Workers (optional / legacy) |
| Build | Maven (multi-stage Docker builds) |

---

## Project structure

```
Mongodb-with-API-testX/
├── tempconv/                    # Temperature microservice
│   ├── src/main/java/com/nima/tempconv/
│   │   ├── controller/          # TemperatureController, HomeController
│   │   ├── service/             # TemperatureService
│   │   ├── repository/          # TemperatureRepository, ApiKeyRepository
│   │   ├── model/               # TemperatureLog, ApiKey
│   │   └── exception/           # UnauthorizedApiKeyException, GlobalExceptionHandler
│   ├── src/main/resources/application.yaml
│   └── Dockerfile
├── currencyconvertor/           # Currency microservice
│   ├── src/main/java/com/usdtolkr/currencyconvertor/
│   │   ├── controller/          # CurrencyController, HomeController
│   │   ├── service/             # CurrencyService
│   │   ├── repository/          # CurrencyRepository
│   │   └── model/               # CurrencyLog
│   ├── src/main/resources/application.yaml
│   └── Dockerfile
├── frontend/                    # Static web UI
│   ├── index.html
│   ├── style.css
│   ├── app.js
│   └── Dockerfile               # nginx:1.27-alpine
├── docs/
│   ├── PROJECT.md               # This file
│   ├── demo.png                 # Screenshot for README
│   ├── mongo-seed-api-keys.js   # API key seed script
│   ├── api endpoints.pdf        # Lab reference
│   └── api keys SpringAPI.pdf   # Lab 05 reference
├── .github/workflows/
│   └── deploy-pages.yml         # Optional Cloudflare Pages CI deploy
├── docker-compose.yml
└── README.md                    # Quick-start guide
```

---

## Services & ports

| Service | Container name | Host port | Purpose |
|---------|----------------|-----------|---------|
| Frontend UI | `converthub-frontend` | **3000** | Web interface — **open this in the browser** |
| Temperature API | `tempconv` | **8081** | REST API only (not the UI) |
| Currency API | `currencyconvertor` | **8082** | REST API only (not the UI) |
| MongoDB (temp) | `mongo-temp` | **27017** | Database `temp_db` |
| MongoDB (currency) | `mongo-currency` | **27018** | Database `currency_db` |
| Mongo seed (one-shot) | `mongo-seed` | — | Seeds `api_keys` on startup |

---

## MongoDB data model

### Database: `temp_db` (port 27017)

#### Collection: `conversions`

Stores temperature conversion history.

| Field | Type | Description |
|-------|------|-------------|
| `_id` | String | MongoDB document ID |
| `inputTemperature` | double | Input value |
| `inputUnit` | String | `Celsius`, `Fahrenheit`, or `Kelvin` |
| `outputTemperature` | double | Converted value |
| `outputUnit` | String | Target unit |
| `timestamp` | String | ISO-8601 timestamp |

#### Collection: `api_keys` (Lab 05)

| Field | Type | Description |
|-------|------|-------------|
| `_id` | String | MongoDB document ID |
| `keyValue` | String | The API key string |
| `owner` | String | Owner label |
| `active` | boolean | Whether the key is valid |
| `createdAt` | String | Creation timestamp |

**Seeded keys:**

| Key | Active | Purpose |
|-----|--------|---------|
| `SUPER-SECRET-DEV-KEY-123` | yes | Valid dev key |
| `EXPIRED-HACKER-KEY-999` | no | Inactive key for testing 401 |

### Database: `currency_db` (port 27018)

#### Collection: `currencyLog` (default collection name)

| Field | Type | Description |
|-------|------|-------------|
| `_id` | String | MongoDB document ID |
| `inputAmount` | double | USD amount |
| `inputCurrency` | String | `USD` |
| `outputAmount` | double | LKR result |
| `outputCurrency` | String | `LKR` |
| `exchangeRate` | double | Rate used (300.0) |
| `timestamp` | String | Conversion timestamp |

---

## Temperature API (port 8081)

Base path: `/api/temperatures`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/` | None | Service info JSON (via `HomeController`) |
| GET | `/api/temperatures/safety-check?value=&unit=` | None | Plain-text safety message (Lab 04) |
| GET | `/api/temperatures/history` | None | All conversion logs |
| GET | `/api/temperatures/history/filter?unit=` | None | Logs filtered by input unit (Lab 04) |
| POST | `/api/temperatures/convert?value=&unit=` | **`X-API-KEY` header** | Convert and save (Lab 05) |

### Supported units

Input `unit` parameter accepts (case-insensitive):

- `celsius`, `c`, `Celsius`
- `fahrenheit`, `f`, `Fahrenheit`
- `kelvin`, `k`, `Kelvin`

### Example responses

**POST convert (200):**

```json
{
  "id": "...",
  "inputTemperature": 25.0,
  "inputUnit": "Celsius",
  "outputTemperature": 77.0,
  "outputUnit": "Fahrenheit",
  "timestamp": "2026-07-01T10:00:00Z"
}
```

**GET safety-check (200, plain text):**

```
Warning: 102.0°F is dangerously HOT! Stay hydrated.
```

**401 unauthorized (missing/invalid key):**

```
Missing required header X-API-KEY
```

or

```
Invalid or inactive API key
```

---

## Currency API (port 8082)

Base path: `/api/currency`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/` | None | Service info JSON |
| POST | `/api/currency/convert?usdAmount=` | None | Convert USD to LKR and save |
| GET | `/api/currency/history` | None | All conversion logs |

### Notes

- Fixed exchange rate: **1 USD = 300 LKR** (demo constant in `CurrencyService`)
- Also accepts typo param `usdAmout` for backward compatibility with Postman tests

### Example response

```json
{
  "id": "...",
  "inputAmount": 100.0,
  "inputCurrency": "USD",
  "outputAmount": 30000.0,
  "outputCurrency": "LKR",
  "exchangeRate": 300.0,
  "timestamp": "2026-07-01T10:00:00Z"
}
```

---

## Lab 04 — Safety check & filtered history

Implemented in `TemperatureService` and exposed via `TemperatureController`.

### Safety check logic

1. Converts input to Fahrenheit and Celsius internally
2. If Fahrenheit **> 100°F** → dangerously hot warning
3. If Celsius is between **15°C and 30°C** → comfortable and safe message
4. Otherwise → outside comfortable range warning

### Filtered history

`GET /api/temperatures/history/filter?unit=celsius` returns only logs where `inputUnit` matches (normalized to `Celsius`, `Fahrenheit`, or `Kelvin`).

### curl examples

```bash
curl "http://localhost:8081/api/temperatures/safety-check?value=102&unit=F"

curl "http://localhost:8081/api/temperatures/safety-check?value=21&unit=C"

curl "http://localhost:8081/api/temperatures/history/filter?unit=celsius"
```

---

## Lab 05 — API key authentication

Only **`POST /api/temperatures/convert`** requires authentication.

### Flow

1. Client sends header: `X-API-KEY: <key>`
2. `TemperatureService.validateApiKey()` checks:
   - Header present and non-empty
   - Key exists in MongoDB `api_keys` collection
   - Key has `active: true`
3. On failure → `401 Unauthorized` with plain-text message

### Key classes

| Class | Role |
|-------|------|
| `ApiKey` | MongoDB document model |
| `ApiKeyRepository` | `findByKeyValue(String)` |
| `UnauthorizedApiKeyException` | Thrown on auth failure |
| `GlobalExceptionHandler` | Maps exceptions to HTTP status |

### curl examples

```bash
# Missing key → 401
curl -X POST "http://localhost:8081/api/temperatures/convert?value=25&unit=celsius"

# Inactive key → 401
curl -X POST "http://localhost:8081/api/temperatures/convert?value=25&unit=celsius" \
  -H "X-API-KEY: EXPIRED-HACKER-KEY-999"

# Valid key → 200
curl -X POST "http://localhost:8081/api/temperatures/convert?value=25&unit=celsius" \
  -H "X-API-KEY: SUPER-SECRET-DEV-KEY-123"
```

### Seed script

```bash
mongosh mongodb://localhost:27017/temp_db docs/mongo-seed-api-keys.js
```

Docker Compose runs this automatically via the `mongo-seed` one-shot service.

---

## Frontend (web UI)

### Files

| File | Purpose |
|------|---------|
| `index.html` | Layout, hero, converter cards, history tables |
| `style.css` | Organic/beige theme, glass navbar, responsive layout |
| `app.js` | API calls, tab switching, toasts, keyboard shortcuts |

### API routing (`app.js`)

```javascript
const isLocalRuntime = ['localhost', '127.0.0.1'].includes(window.location.hostname);

// Local → Spring Boot on 8081/8082
// Production → Cloudflare Workers
const TEMP_API_KEY = 'SUPER-SECRET-DEV-KEY-123';
```

Temperature convert sends:

```javascript
headers: { 'X-API-KEY': TEMP_API_KEY }
```

### UI features

- Floating glass navigation with Currency / Temperature tabs
- Hero section with feature chips
- Conversion result panels with formatted numbers
- History tables (newest first)
- Toast notifications (success / error)
- Enter key to submit active converter
- Skip link and ARIA tab roles for accessibility
- Reduced-motion support in CSS

### Design palette (organic theme)

| Token | Color | Usage |
|-------|-------|-------|
| Background | Linen beige `#EDE6DB` | Page background |
| Headline accent | Forest green `#1F4D3C` | Hero emphasis |
| Headline body | Charcoal `#3C3C3D` | Primary text |
| Body text | Grey `#6C6C6C` | Subtitles |
| Navbar / footer | Bronze-taupe `#4D4940` | Chrome |
| Currency accent | Teal `#0D6C68` | Currency tab / actions |
| Temperature accent | Grey `#8C8983` | Temperature tab |
| Warm accents | Terracotta, golden-tan | Feature dots |

### Cache busting

`index.html` loads `app.js?v=2` to force browsers to pick up API key changes after deploy.

---

## Docker setup

### Start full stack

```bash
docker compose up --build
```

Open **http://localhost:3000** (not 8081 or 8082).

### Stop

```bash
docker compose down
```

### Rebuild frontend only

```bash
docker compose build --no-cache frontend
docker compose up -d frontend
```

### Compose services

| Service | Image / build | Depends on |
|---------|---------------|------------|
| `mongo-temp` | `mongo:7.0` | — |
| `mongo-currency` | `mongo:7.0` | — |
| `mongo-seed` | `mongo:7.0` (one-shot) | `mongo-temp` healthy |
| `tempconv` | Built from `./tempconv` | Mongo + seed complete |
| `currencyconvertor` | Built from `./currencyconvertor` | `mongo-currency` healthy |
| `frontend` | Built from `./frontend` (nginx) | Both APIs |

Persistent volumes: `mongo-temp-data`, `mongo-currency-data`

---

## Local development (without Docker)

**Prerequisites:** Java 21, Maven, MongoDB 6+

1. Start MongoDB on ports `27017` and `27018`
2. Seed API keys:
   ```bash
   mongosh mongodb://localhost:27017/temp_db docs/mongo-seed-api-keys.js
   ```
3. Run temperature service:
   ```bash
   cd tempconv
   ./mvnw spring-boot:run
   ```
4. Run currency service:
   ```bash
   cd currencyconvertor
   ./mvnw spring-boot:run
   ```
5. Serve `frontend/` (e.g. Live Server, or `npx serve frontend`) and open in browser

---

## Deployment

### Frontend — Cloudflare Pages (manual, from repo root)

```bash
npx wrangler login
npx wrangler pages deploy frontend --project-name=converthub-mongodb-api
```

| Setting | Value |
|---------|-------|
| Project name | `converthub-mongodb-api` |
| Custom domain | `api.adheesha.dev` |
| Account ID | `b825b696a451c013754c5311869a128f` |

### Frontend — GitHub Actions (optional)

Workflow: `.github/workflows/deploy-pages.yml`

Requires repository secret: `CLOUDFLARE_API_TOKEN` with **Cloudflare Pages — Edit** permission.

Triggers on push to `main` when `frontend/**` changes, or via **workflow_dispatch**.

### Backend

Spring Boot services and MongoDB are **not** deployed to Cloudflare. They run locally via Docker Compose. Production demo uses Cloudflare Workers for API calls unless you host the Spring Boot stack elsewhere.

---

## Environment variables

### tempconv

| Variable | Default | Description |
|----------|---------|-------------|
| `MONGODB_URI` | `mongodb://localhost:27017/temp_db` | MongoDB connection |
| `SERVER_PORT` | `8081` | HTTP port |

### currencyconvertor

| Variable | Default | Description |
|----------|---------|-------------|
| `MONGODB_URI` | `mongodb://localhost:27017/currency_db` | MongoDB connection |
| `SERVER_PORT` | `8082` | HTTP port |

> Spring Boot 4 uses `spring.mongodb.uri` (not `spring.data.mongodb.uri`).

---

## Conversion logic

### Temperature

| Input unit | Output unit | Formula |
|------------|-------------|---------|
| Celsius | Fahrenheit | `(C × 1.8) + 32` |
| Fahrenheit | Celsius | `(F − 32) / 1.8` |
| Kelvin | Celsius | `K − 273.15` |

### Currency

```
LKR = USD × 300
```

---

## Troubleshooting

### 404 when opening `localhost:8081` or `8082`

Ports **8081** and **8082** are REST APIs, not the web UI. Open **http://localhost:3000** for the frontend.

Root `/` on each API returns JSON service info instead of 404.

### `Missing required header X-API-KEY`

- **Local Docker:** Rebuild the frontend container so it serves the latest `app.js` with the API key header:
  ```bash
  docker compose build --no-cache frontend && docker compose up -d frontend
  ```
- Hard refresh the browser (`Ctrl+F5`)
- **Production:** Redeploy frontend to Cloudflare Pages (see [Deployment](#deployment))

### Temperature convert works on production but not locally

Production uses Cloudflare Workers (no Lab 05 key required). Local Docker uses Spring Boot which **requires** `X-API-KEY`.

### MongoDB connection errors in Docker

Ensure `application.yaml` uses `spring.mongodb.uri`. Verify containers are healthy:

```bash
docker compose ps
```

### API keys not found

Re-run seed:

```bash
mongosh mongodb://localhost:27017/temp_db docs/mongo-seed-api-keys.js
```

Or restart stack so `mongo-seed` runs again (may need `docker compose down -v` for fresh DB).

### Wrangler deploy fails

- Run `npx wrangler login` in an interactive terminal
- Or set `CLOUDFLARE_API_TOKEN` environment variable
- Disable VPN/proxy if OAuth callback fails with certificate errors

---

## Related files in `docs/`

| File | Description |
|------|-------------|
| `demo.png` | Screenshot used in README |
| `mongo-seed-api-keys.js` | Seeds `api_keys` collection |
| `api endpoints.pdf` | Lab endpoint specifications |
| `api keys SpringAPI.pdf` | Lab 05 API key requirements |

---

*Last updated: July 2026*
