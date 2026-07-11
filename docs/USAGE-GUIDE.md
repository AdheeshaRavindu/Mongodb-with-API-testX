# ConvertHub — Complete Usage Guide

Step-by-step instructions for running, testing, and troubleshooting **ConvertHub** locally and on the live site.

**Related docs:** [README](../README.md) (quick start) · [DOCKER-GUIDE](DOCKER-GUIDE.md) (Docker commands) · [PROJECT.md](PROJECT.md) (technical reference)

---

## Table of contents

1. [Before you start](#1-before-you-start)
2. [Start the app (Docker)](#2-start-the-app-docker)
3. [Use the web UI](#3-use-the-web-ui)
4. [MongoDB Compass setup](#4-mongodb-compass-setup)
5. [Test with Postman](#5-test-with-postman)
6. [Test with curl](#6-test-with-curl)
7. [Authentication & RabbitMQ](#7-authentication--rabbitmq)
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
| **27017** | MongoDB for temperature | No — use Compass |
| **27018** | MongoDB for currency | No — use Compass |
| **56720** | RabbitMQ AMQP (host; Docker network uses 5672) | No |
| **15672** | RabbitMQ management UI | Yes — guest/guest |

> **Important:** Ports **8081** and **8082** are APIs only. Opening them in a browser shows JSON, not the converter UI. Always use **http://localhost:3000** for the web app.

### Live demo (no Docker needed)

- **https://api.adheesha.dev**
- Uses Cloudflare Workers for APIs (see [section 8](#8-live-site-vs-local-docker))

---

## 2. Start the app (Docker)

### One-time setup: `.env` file

Docker Compose reads [`.env`](../.env) from the project root. Create it **once** (not every run):

```powershell
copy .env.example .env
```

Edit `.env` and set your real Google Client ID and JWT secret:

```env
GOOGLE_CLIENT_ID=xxxx.apps.googleusercontent.com
JWT_SECRET=your-long-random-secret-at-least-32-characters
JWT_EXPIRATION_HOURS=24
```

> **Important:** If `.env` is missing or uses placeholders, login (`POST /auth/google`) or protected APIs may return **401**.

After changing `.env`, recreate the API containers (no full rebuild needed):

```powershell
docker compose up -d --force-recreate tempconv currencyconvertor
```

### Commands

From the project root (`Mongodb-with-API-testX/`):

```powershell
# First time or after code changes
docker compose up --build -d

# Normal run (no code changes)
docker compose up -d
```

RabbitMQ alone (without full compose):

```bash
docker run -d --name rabbitmq -p 56720:5672 -p 15672:15672 rabbitmq:3.13-management
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
| `mongo-temp` | MongoDB | Stores `temp_db` on port **27017** |
| `mongo-currency` | MongoDB | Stores `currency_db` on port **27018** |
| `mongo-seed` | One-shot | Seeds demo data into `temp_db`, then exits |
| `mongo-currency-seed` | One-shot | Seeds demo data into `currency_db`, then exits |
| `rabbitmq` | RabbitMQ | Broker AMQP on host **56720** (container **5672**), management UI on **15672** |
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
2. Click **Sign in** in the auth bar (or open [http://localhost:3000/get-token.html](http://localhost:3000/get-token.html))
3. Sign in with Google — the frontend exchanges your Google ID token with `POST /auth/google` and stores the **application JWT**
4. Status should change to `Signed in as <your name>`
5. Use Currency / Temperature converters as usual

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
4. The frontend sends `Authorization: Bearer <application_jwt>` — sign in first
5. View results and history below

### Tips

- Press **Enter** while focused on an input field to convert
- If you see `401 Unauthorized`, use the **Sign in with Google** button in the auth bar on the home page, then hard refresh: **Ctrl+F5**
- Click **Logout** in the auth bar to clear the stored JWT
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

Obtain an **application JWT** first (see [section 7](#7-authentication--rabbitmq)).

All `/api/**` requests need:

| Header | Value |
|--------|-------|
| `Authorization` | `Bearer YOUR_APPLICATION_JWT` |

### Temperature API (port 8081)

Base URL: `http://localhost:8081/api/temperatures`

#### POST convert — success (200)

| Setting | Value |
|---------|-------|
| **Method** | `POST` |
| **URL** | `http://localhost:8081/api/temperatures/convert?value=100&unit=Celsius` |
| **Headers** | `Authorization` = `Bearer YOUR_APPLICATION_JWT` |

#### POST convert — missing token (401)

| Setting | Value |
|---------|-------|
| **Method** | `POST` |
| **URL** | Same as above |
| **Headers** | None |

Expected: HTTP **401 Unauthorized**.

#### GET safety check — Bearer required

| Setting | Value |
|---------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8081/api/temperatures/safety-check?value=102&unit=F` |
| **Headers** | `Authorization: Bearer …` |

#### GET history / filter — Bearer required

| Setting | Value |
|---------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8081/api/temperatures/history` or `.../history/filter?unit=celsius` |
| **Headers** | `Authorization: Bearer …` |

---

### Currency API (port 8082)

Base URL: `http://localhost:8082/api/currency`

#### POST convert (200)

| Setting | Value |
|---------|-------|
| **Method** | `POST` |
| **URL** | `http://localhost:8082/api/currency/convert?usdAmount=100` |
| **Headers** | `Authorization: Bearer YOUR_APPLICATION_JWT` |

#### GET history

| Setting | Value |
|---------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8082/api/currency/history` |
| **Headers** | `Authorization: Bearer YOUR_APPLICATION_JWT` |

---

## 6. Test with curl

```bash
# Step 1: Exchange Google ID token for application JWT
curl -X POST "http://localhost:8081/auth/google" \
  -H "Content-Type: application/json" \
  -d '{"idToken":"YOUR_GOOGLE_ID_TOKEN"}'

# Step 2: Use application JWT on protected APIs
TOKEN="YOUR_APPLICATION_JWT"

# Missing token → 401
curl -X POST "http://localhost:8081/api/temperatures/convert?value=25&unit=celsius"

# Temperature convert → 200
curl -X POST "http://localhost:8081/api/temperatures/convert?value=25&unit=celsius" \
  -H "Authorization: Bearer $TOKEN"

# Safety check
curl "http://localhost:8081/api/temperatures/safety-check?value=102&unit=F" \
  -H "Authorization: Bearer $TOKEN"

# History filter
curl "http://localhost:8081/api/temperatures/history/filter?unit=celsius" \
  -H "Authorization: Bearer $TOKEN"

# Currency convert → 200 (also publishes RabbitMQ event)
curl -X POST "http://localhost:8082/api/currency/convert?usdAmount=100" \
  -H "Authorization: Bearer $TOKEN"

curl "http://localhost:8082/api/currency/history" \
  -H "Authorization: Bearer $TOKEN"

# Public home (no auth)
curl "http://localhost:8081/"
curl "http://localhost:8082/"
```

---

## 7. Authentication & RabbitMQ

ConvertHub uses a **two-step authentication** flow:

1. **Login (Google only):** The frontend uses Google Identity Services to obtain a Google ID token, then sends it once to `POST /auth/google` on the Temperature API (**8081**).
2. **API access (application JWT):** The backend verifies the Google token, creates/updates the user in MongoDB, and returns an **application-issued JWT**. All `/api/**` calls use `Authorization: Bearer <application_jwt>`.

Google ID tokens are **not** sent to protected APIs. Both Temperature (**8081**) and Currency (**8082**) validate the **same** application JWT (shared `JWT_SECRET`).

### Google Cloud setup (Client ID)

1. Open [Google Cloud Console → Credentials](https://console.cloud.google.com/apis/credentials).
2. Create an **OAuth 2.0 Client ID** of type **Web application**.
3. Under **Authorized JavaScript origins**, add at least:
   - `http://localhost:3000`
   - `http://localhost` (optional)
4. Copy the **Client ID** (ends with `.apps.googleusercontent.com`).
5. Put it in the project root [`.env`](../.env) file (Compose reads this automatically):

```env
GOOGLE_CLIENT_ID=xxxx.apps.googleusercontent.com
JWT_SECRET=your-long-random-secret-at-least-32-characters
JWT_EXPIRATION_HOURS=24
```

Or set it in PowerShell before `docker compose up`:

```powershell
$env:GOOGLE_CLIENT_ID="xxxx.apps.googleusercontent.com"
$env:JWT_SECRET="your-long-random-secret"
docker compose up --build
```

Leave **Client Secret** unused for this flow.

### How to get an application JWT (step by step)

1. Start the stack (`docker compose up -d`) and confirm `.env` has your real `GOOGLE_CLIENT_ID` and `JWT_SECRET`.
2. From the home page, click **Sign in** — or open **http://localhost:3000/get-token.html** directly.
3. Click **Sign in with Google**, choose your account, and allow access.
4. The frontend calls `POST http://localhost:8081/auth/google` with `{ "idToken": "..." }` and stores the returned application JWT.
5. You are redirected to the home page. The auth bar shows your name and **Logout**.

**For Postman:** After signing in on the home page, click the small **Copy API token** link in the auth bar. Or use DevTools → Application → Local Storage → `converthub_app_jwt`.

**Do not commit or share JWTs publicly** — treat them like passwords until they expire.

### Use the application JWT

#### ConvertHub UI

1. Home → **Sign in** → complete Google sign-in
2. Convert as usual on **http://localhost:3000**
3. On **401**, you are redirected to sign in again

#### Postman (important)

Do **not** choose Auth Type **OAuth 2.0** (that asks for Auth URL / Client Secret and often fails with "Invalid protocol for auth URL").

**Option A — sign in via browser (recommended):**

1. Sign in on the home page (Google button in the auth bar)
2. Click **Copy API token** in the auth bar (small link next to your name)
3. In Postman: **Authorization** tab → Auth Type → **Bearer Token**
4. Paste the **application JWT** (Postman adds the `Bearer` prefix)
5. Send protected requests — expect **200** JSON

**Option B — exchange manually:**

1. `POST http://localhost:8081/auth/google` with body `{ "idToken": "<google_id_token>" }`
2. Copy `token` from the JSON response
3. Use as Bearer token on `http://localhost:8081/api/temperatures/...` and `http://localhost:8082/api/currency/...`

#### curl (PowerShell)

```powershell
# Exchange Google ID token (from GIS sign-in) for application JWT
$auth = Invoke-RestMethod -Method POST -Uri "http://localhost:8081/auth/google" `
  -ContentType "application/json" `
  -Body '{"idToken":"PASTE_GOOGLE_ID_TOKEN_HERE"}'
$token = $auth.token

curl.exe -X POST "http://localhost:8081/api/temperatures/convert?value=25&unit=celsius" `
  -H "Authorization: Bearer $token"

curl.exe -X POST "http://localhost:8082/api/currency/convert?usdAmount=100" `
  -H "Authorization: Bearer $token"
```

When calls start returning **401**, sign in again or exchange a fresh Google ID token.

### What RabbitMQ does in this project

RabbitMQ is used as an **event bus after a successful convert**. It does **not** perform the conversion itself.

```text
Client → API converts + saves MongoDB → publishes message → RabbitMQ → consumer logs it
```

| When | What happens |
|------|----------------|
| `POST /api/temperatures/convert` succeeds | Event published to `temperature.conversion.queue` |
| `POST /api/currency/convert` succeeds | Event published to `currency.conversion.queue` |

History / safety-check GETs do **not** publish. MongoDB remains the source of truth for conversion history; RabbitMQ is for async notify / audit / demo messaging.

### RabbitMQ configuration

| Setting | Value |
|---------|-------|
| Host | `localhost` (or `rabbitmq` in Docker Compose) |
| AMQP port (host) | `56720` (container internal: `5672`) |
| Management UI | http://localhost:15672 |
| Username / password | `guest` / `guest` |
| Exchange | `converthub.exchange` (topic) |
| Temp queue | `temperature.conversion.queue` (routing key `conversion.temperature`) |
| Currency queue | `currency.conversion.queue` (routing key `conversion.currency`) |

Standalone broker:

```bash
docker run -d --name rabbitmq -p 56720:5672 -p 15672:15672 rabbitmq:3.13-management
```

### How to check RabbitMQ

#### 1. Management UI (easiest)

1. Open: **http://localhost:15672**
2. Login: **guest** / **guest**
3. Go to **Queues** and find:
   - `temperature.conversion.queue`
   - `currency.conversion.queue`
4. Perform a convert with a valid Bearer token, then refresh — message totals / rates should increase.
5. Under **Exchanges**, confirm `converthub.exchange` exists.

#### 2. API logs

```powershell
docker compose logs -f tempconv
docker compose logs -f currencyconvertor
```

After a successful convert you should see lines like:

- `Published … conversion event`
- `Received … conversion event`

#### 3. Confirm the broker container

```powershell
docker compose ps rabbitmq
```

Status should be **Up (healthy)**. Ports: **56720** (AMQP on host), **15672** (management UI).

### Verify RabbitMQ after convert

1. Call a convert endpoint with a valid Bearer token (UI or Postman).
2. Check API logs for `Published …` and `Received … conversion event`.
3. Or open http://localhost:15672 → **Queues** → inspect message rates on the conversion queues.

---

## 8. Live site vs local Docker

### What Cloudflare deploys

Cloudflare Pages hosts **only the frontend** (`index.html`, `style.css`, `app.js`).

It does **not** deploy:

- Spring Boot services
- MongoDB
- RabbitMQ
- Application JWT validation on local APIs (via shared `JWT_SECRET`)

### Where production APIs come from

When you visit **https://api.adheesha.dev**, the frontend calls **Cloudflare Workers**, not your local Docker APIs:

| Service | Production URL |
|---------|----------------|
| Currency | `https://currency-converter.vikumkodikara123.workers.dev/api/currency` |
| Temperature | `https://temperature-converter.vikumkodikara123.workers.dev/api/temperatures` |

When you visit **http://localhost:3000**, the frontend calls **localhost:8081** and **8082** instead (with application JWT Bearer token).

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

### `401 Unauthorized` on `/api/**`

**Check `.env` first:** APIs must have matching `JWT_SECRET` on both services, and tempconv must have a real `GOOGLE_CLIENT_ID` for login.

```powershell
docker inspect tempconv --format "{{range .Config.Env}}{{println .}}{{end}}" | Select-String "GOOGLE|JWT"
docker inspect currencyconvertor --format "{{range .Config.Env}}{{println .}}{{end}}" | Select-String "JWT"
```

If wrong, fix `.env` then:

```powershell
docker compose up -d --force-recreate tempconv currencyconvertor
```

**In Postman:** Use Auth Type **Bearer Token** with the **application JWT** (not the Google ID token). Sign in at http://localhost:3000/get-token.html and copy `converthub_app_jwt` from Local Storage, or call `POST /auth/google` manually.

**In the web UI:** Click **Sign in**, complete Google sign-in, then hard refresh (**Ctrl+F5**) if needed. Rebuild frontend if UI is stale:

```bash
docker compose build --no-cache frontend
docker compose up -d frontend
```

### Postman “Invalid protocol for auth URL” or OAuth errors

You selected Auth Type **OAuth 2.0**. Switch to **Bearer Token** and paste the **application JWT** from `converthub_app_jwt` (after signing in at `/get-token.html`) or from `POST /auth/google`. You do not need Auth URL, Access Token URL, or Client Secret for protected API calls.

### `ECONNREFUSED` on 8081 / 8082

APIs are not running. From the project root:

```bash
docker compose ps
docker compose logs tempconv
docker compose logs currencyconvertor
docker compose up --build -d
```

Both services should show **Up**, not **Restarting**.

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

Root `/` on each API returns JSON service info — that is normal (and does not require auth).

---

### RabbitMQ connection errors on startup

Ensure the broker is up (`docker compose ps` → `rabbitmq` healthy, or the standalone `docker run` command). Management UI: http://localhost:15672.

---

### Docker containers not starting

```bash
docker compose ps
docker compose logs tempconv
docker compose logs rabbitmq
docker compose logs mongo-temp
```

Ensure ports 3000, 8081, 8082, 27017, 27018, 56720, and 15672 are not in use by other apps.

---

## 10. Quick reference

### All endpoints

| Service | Method | URL | Auth | Port |
|---------|--------|-----|------|------|
| Temperature | GET | `/` | None | 8081 |
| Temperature | POST | `/auth/google` | None (body: Google `idToken`) | 8081 |
| Temperature | GET | `/api/temperatures/safety-check?value=&unit=` | Bearer (app JWT) | 8081 |
| Temperature | GET | `/api/temperatures/history` | Bearer (app JWT) | 8081 |
| Temperature | GET | `/api/temperatures/history/filter?unit=` | Bearer (app JWT) | 8081 |
| Temperature | POST | `/api/temperatures/convert?value=&unit=` | Bearer (app JWT) | 8081 |
| Currency | GET | `/` | None | 8082 |
| Currency | POST | `/api/currency/convert?usdAmount=` | Bearer (app JWT) | 8082 |
| Currency | GET | `/api/currency/history` | Bearer (app JWT) | 8082 |

### MongoDB locations

| Port | Database | Collection | Contents |
|------|----------|------------|----------|
| 27017 | `temp_db` | `conversions` | Temperature history |
| 27018 | `currency_db` | (default) | Currency history |

### Environment placeholders

| Variable | Example |
|----------|---------|
| `GOOGLE_CLIENT_ID` | `xxxx.apps.googleusercontent.com` (login on tempconv) |
| `JWT_SECRET` | Long random secret (must match on both APIs) |
| `JWT_EXPIRATION_HOURS` | `24` |
| `RABBITMQ_HOST` | `localhost` or `rabbitmq` |
| `RABBITMQ_PORT` | `5672` (inside Docker Compose; use `56720` from host) |
| `RABBITMQ_USER` / `RABBITMQ_PASS` | `guest` / `guest` |

### Common commands

```bash
# PowerShell
$env:GOOGLE_CLIENT_ID="xxxx.apps.googleusercontent.com"
$env:JWT_SECRET="your-long-random-secret"
docker compose up --build          # Start stack
docker compose down                # Stop stack
docker compose ps                  # Check status
docker compose logs -f tempconv    # View temperature API logs
docker run -d --name rabbitmq -p 56720:5672 -p 15672:15672 rabbitmq:3.13-management
```

---

*Last updated: July 2026*
