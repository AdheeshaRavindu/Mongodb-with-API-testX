# ConvertHub — Complete Usage Guide

Step-by-step instructions for running, testing, and troubleshooting **ConvertHub** locally and on the live site.

**Related docs:** [README](../README.md) (quick start) · [PROJECT.md](PROJECT.md) (technical reference)

---

## Table of contents

1. [Before you start](#1-before-you-start)
2. [Start the app (Docker)](#2-start-the-app-docker)
3. [Use the web UI](#3-use-the-web-ui)
4. [MongoDB Compass setup](#4-mongodb-compass-setup)
5. [Test with Postman](#5-test-with-postman)
6. [Test with curl](#6-test-with-curl)
7. [API keys explained](#7-api-keys-explained)
8. [Live site vs local Docker](#8-live-site-vs-local-docker)
9. [Troubleshooting (FAQ)](#9-troubleshooting-faq)
10. [Quick reference](#10-quick-reference)

---

## 1. Before you start

### Prerequisites

| Tool | Required? | Purpose |
|------|-----------|---------|
| **Docker Desktop** | Yes (recommended) | Run the full stack with one command |
| **Web browser** | Yes | Use the UI at http://localhost:3000 |
| **Postman** | Optional | Test API endpoints manually |
| **MongoDB Compass** | Optional | View databases and API keys |
| **Java 21 + Maven** | Only without Docker | Run Spring Boot services manually |

### Port map (memorize this)

| Port | What it is | Open in browser? |
|------|------------|------------------|
| **3000** | Web UI (frontend) | **Yes — start here** |
| **8081** | Temperature REST API | No — use Postman/curl |
| **8082** | Currency REST API | No — use Postman/curl |
| **27017** | MongoDB for temperature + API keys | No — use Compass |
| **27018** | MongoDB for currency | No — use Compass |

> **Important:** Ports **8081** and **8082** are APIs only. Opening them in a browser shows JSON, not the converter UI. Always use **http://localhost:3000** for the web app.

### Live demo (no Docker needed)

- **https://api.adheesha.dev**
- Uses Cloudflare Workers for APIs (see [section 8](#8-live-site-vs-local-docker))

---

## 2. Start the app (Docker)

### Commands

From the project root (`Mongodb-with-API-testX/`):

```bash
# Start everything (build + run)
docker compose up --build
```

Run in the background:

```bash
docker compose up --build -d
```

Check status:

```bash
docker compose ps
```

Stop everything:

```bash
docker compose down
```

### What each container does

| Container | Service | Role |
|-----------|---------|------|
| `mongo-temp` | MongoDB | Stores `temp_db` (temperature history + API keys) on port **27017** |
| `mongo-currency` | MongoDB | Stores `currency_db` on port **27018** |
| `mongo-seed` | One-shot | Seeds `api_keys` into `temp_db`, then exits |
| `tempconv` | Spring Boot | Temperature API on port **8081** |
| `currencyconvertor` | Spring Boot | Currency API on port **8082** |
| `converthub-frontend` | nginx | Web UI on port **3000** |

### Verify before testing

Run `docker compose ps`. All services except `mongo-seed` should show **Up**. `mongo-seed` should show **Exited (0)** — that is normal.

Then open **http://localhost:3000**.

---

## 3. Use the web UI

### Open the app

1. Go to **http://localhost:3000**
2. You should see the ConvertHub hero, navbar, and converter cards

### Currency converter

1. Click the **Currency** tab (default)
2. Enter a USD amount (e.g. `100`)
3. Click **Convert**
4. Result shows LKR amount (rate: 1 USD = 300 LKR)
5. Scroll down to see **Conversion History**

### Temperature converter

1. Click the **Temperature** tab
2. Enter a value (e.g. `100`) and select a unit (Celsius, Fahrenheit, or Kelvin)
3. Click **Convert**
4. The frontend automatically sends the API key header — no manual setup needed
5. View results and history below

### Tips

- Press **Enter** while focused on an input field to convert
- If you see `Missing required header X-API-KEY`, hard refresh: **Ctrl+F5**
- If the UI looks outdated after a code change, rebuild the frontend container (see [Troubleshooting](#9-troubleshooting-faq))

---

## 4. MongoDB Compass setup

### Connect to both databases

| Connection string | Database | Collections |
|-------------------|----------|-------------|
| `mongodb://localhost:27017` | `temp_db` | `api_keys`, `conversions` |
| `mongodb://localhost:27018` | `currency_db` | currency history documents |

### View API keys

1. Connect to **localhost:27017**
2. Open **`temp_db`** → **`api_keys`**
3. You should see two documents:

| keyValue | active | Purpose |
|----------|--------|---------|
| `SUPER-SECRET-DEV-KEY-123` | `true` | Valid key for convert |
| `EXPIRED-HACKER-KEY-999` | `false` | Test inactive key (401) |

### Can't see `temp_db`? (Windows port conflict)

If Compass on **27017** shows `test` but not `temp_db`, a **local MongoDB service** may be blocking Docker.

**Fix (PowerShell as Admin):**

```powershell
net stop MongoDB
```

Then **Refresh** the connection in Compass. You should now see **`temp_db`**.

> Port **27018** usually works without issues because nothing else uses it.

### Re-seed API keys

If you deleted keys or need to reset:

```bash
docker run --rm --network mongodb-with-api-testx_default -v "%cd%/docs/mongo-seed-api-keys.js:/seed.js:ro" mongo:7.0 mongosh mongodb://mongo-temp:27017/temp_db --file /seed.js
```

Or from the host (if `mongosh` is installed and Docker mongo is on 27017):

```bash
mongosh mongodb://localhost:27017/temp_db docs/mongo-seed-api-keys.js
```

---

## 5. Test with Postman

### Temperature API (port 8081)

Base URL: `http://localhost:8081/api/temperatures`

#### POST convert — success (200)

| Setting | Value |
|---------|-------|
| **Method** | `POST` |
| **URL** | `http://localhost:8081/api/temperatures/convert?value=100&unit=Celsius` |
| **Headers** | `X-API-KEY` = `SUPER-SECRET-DEV-KEY-123` |

In Postman: go to the **Headers** tab → add key `X-API-KEY` and value `SUPER-SECRET-DEV-KEY-123`.

#### POST convert — missing key (401)

| Setting | Value |
|---------|-------|
| **Method** | `POST` |
| **URL** | Same as above |
| **Headers** | None |

Expected body: `Missing required header X-API-KEY`

#### POST convert — inactive key (401)

| Setting | Value |
|---------|-------|
| **Method** | `POST` |
| **URL** | Same as above |
| **Headers** | `X-API-KEY` = `EXPIRED-HACKER-KEY-999` |

Expected body: `Invalid or inactive API key`

#### GET safety check (Lab 04) — no key needed

| Setting | Value |
|---------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8081/api/temperatures/safety-check?value=102&unit=F` |

Expected: plain text warning about hot temperature.

#### GET filtered history (Lab 04) — no key needed

| Setting | Value |
|---------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8081/api/temperatures/history/filter?unit=celsius` |

#### GET full history — no key needed

| Setting | Value |
|---------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8081/api/temperatures/history` |

---

### Currency API (port 8082)

Base URL: `http://localhost:8082/api/currency`

No API key required for any currency endpoint.

#### POST convert (200)

| Setting | Value |
|---------|-------|
| **Method** | `POST` |
| **URL** | `http://localhost:8082/api/currency/convert?usdAmount=100` |
| **Headers** | None |

#### GET history

| Setting | Value |
|---------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8082/api/currency/history` |

---

## 6. Test with curl

### Lab 04 — Safety check and filtered history

```bash
curl "http://localhost:8081/api/temperatures/safety-check?value=102&unit=F"
# → Warning: 102.0°F is dangerously HOT! Stay hydrated.

curl "http://localhost:8081/api/temperatures/safety-check?value=21&unit=C"
# → The temperature is comfortable and safe.

curl "http://localhost:8081/api/temperatures/history/filter?unit=celsius"
# → JSON array of Celsius logs only
```

### Lab 05 — API key on temperature convert

```bash
# Missing key → 401
curl -X POST "http://localhost:8081/api/temperatures/convert?value=25&unit=celsius"

# Invalid/inactive key → 401
curl -X POST "http://localhost:8081/api/temperatures/convert?value=25&unit=celsius" \
  -H "X-API-KEY: EXPIRED-HACKER-KEY-999"

# Valid key → 200
curl -X POST "http://localhost:8081/api/temperatures/convert?value=25&unit=celsius" \
  -H "X-API-KEY: SUPER-SECRET-DEV-KEY-123"
```

### Currency

```bash
curl -X POST "http://localhost:8082/api/currency/convert?usdAmount=100"

curl "http://localhost:8082/api/currency/history"
```

---

## 7. API keys explained

### Where keys are stored

API keys exist **only** on the temperature MongoDB instance:

```
localhost:27017 → temp_db → api_keys
```

Port **27018** / `currency_db` has **no** API keys. Currency endpoints do not use keys.

### Which endpoints need a key?

| Endpoint | API key required? |
|----------|---------------------|
| `POST /api/temperatures/convert` | **Yes** |
| All other temperature endpoints | No |
| All currency endpoints | No |

### What happens if you delete a key?

If you remove `SUPER-SECRET-DEV-KEY-123` from Compass, temperature convert returns **401** until you re-seed.

### Is the seed file a secret?

For this **lab/demo project**, keeping keys in `docs/mongo-seed-api-keys.js` is fine. They are intentional dev values from Lab 05, not production secrets. The frontend also hardcodes the same key in `app.js` (visible in the browser).

For a real production API, you would use environment variables and never commit actual secrets.

---

## 8. Live site vs local Docker

### What Cloudflare deploys

Cloudflare Pages hosts **only the frontend** (`index.html`, `style.css`, `app.js`).

It does **not** deploy:

- Spring Boot services
- MongoDB
- API keys in `temp_db`

### Where production APIs come from

When you visit **https://api.adheesha.dev**, the frontend calls **Cloudflare Workers**, not your local Docker APIs:

| Service | Production URL |
|---------|----------------|
| Currency | `https://currency-converter.vikumkodikara123.workers.dev/api/currency` |
| Temperature | `https://temperature-converter.vikumkodikara123.workers.dev/api/temperatures` |

When you visit **http://localhost:3000**, the frontend calls **localhost:8081** and **8082** instead.

### Lab 05 and production

MongoDB-backed API key validation (Lab 05) runs in your **local Spring Boot** temperature service. The live Cloudflare Workers are separate and may not enforce the same key rules unless updated separately.

### Deploy frontend to Cloudflare

```bash
npx wrangler login
npx wrangler pages deploy frontend --project-name=converthub-mongodb-api
```

Live URLs after deploy:

- https://api.adheesha.dev
- https://converthub-mongodb-api.pages.dev

---

## 9. Troubleshooting (FAQ)

### `Missing required header X-API-KEY`

**In Postman:** Add header `X-API-KEY: SUPER-SECRET-DEV-KEY-123` on POST convert.

**In the web UI:** Rebuild the frontend container and hard refresh:

```bash
docker compose build --no-cache frontend
docker compose up -d frontend
```

Then press **Ctrl+F5** in the browser.

---

### Can't see `temp_db` in Compass (only see `test`)

Local Windows MongoDB is likely using port 27017 instead of Docker.

```powershell
net stop MongoDB
```

Refresh Compass on `localhost:27017`.

---

### 404 or JSON when opening 8081/8082 in browser

Those ports are REST APIs, not the UI. Use **http://localhost:3000**.

Root `/` on each API returns JSON service info — that is normal.

---

### Temperature works on live site but not locally

Different backends:

- **Live site** → Cloudflare Workers
- **Local** → Spring Boot with Lab 05 API key check

Test local convert with the `X-API-KEY` header and ensure keys exist in MongoDB.

---

### API keys missing or deleted

Re-seed:

```bash
mongosh mongodb://localhost:27017/temp_db docs/mongo-seed-api-keys.js
```

Or restart Docker so `mongo-seed` runs again:

```bash
docker compose down
docker compose up --build
```

---

### Docker containers not starting

```bash
docker compose ps
docker compose logs tempconv
docker compose logs mongo-temp
```

Ensure ports 3000, 8081, 8082, 27017, and 27018 are not in use by other apps.

---

## 10. Quick reference

### All endpoints

| Service | Method | URL | Auth | Port |
|---------|--------|-----|------|------|
| Temperature | GET | `/api/temperatures/safety-check?value=&unit=` | None | 8081 |
| Temperature | GET | `/api/temperatures/history` | None | 8081 |
| Temperature | GET | `/api/temperatures/history/filter?unit=` | None | 8081 |
| Temperature | POST | `/api/temperatures/convert?value=&unit=` | `X-API-KEY` | 8081 |
| Currency | POST | `/api/currency/convert?usdAmount=` | None | 8082 |
| Currency | GET | `/api/currency/history` | None | 8082 |

### MongoDB locations

| Port | Database | Collection | Contents |
|------|----------|------------|----------|
| 27017 | `temp_db` | `api_keys` | Lab 05 API keys |
| 27017 | `temp_db` | `conversions` | Temperature history |
| 27018 | `currency_db` | (default) | Currency history |

### Seeded API keys

| Key | Active |
|-----|--------|
| `SUPER-SECRET-DEV-KEY-123` | yes |
| `EXPIRED-HACKER-KEY-999` | no |

### Common commands

```bash
docker compose up --build          # Start stack
docker compose down                # Stop stack
docker compose ps                  # Check status
docker compose logs -f tempconv    # View temperature API logs
```

---

*Last updated: July 2026*
