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
7. [Google OAuth 2.0 & RabbitMQ](#7-google-oauth-20--rabbitmq)
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
| **5672** | RabbitMQ AMQP | No |
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

Edit `.env` and set your real Google Client ID:

```env
GOOGLE_CLIENT_ID=xxxx.apps.googleusercontent.com
```

> **Important:** If `.env` is missing, APIs use `YOUR_GOOGLE_CLIENT_ID` and every request returns **401**, even with a valid token.

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
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management
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
| `rabbitmq` | RabbitMQ | Broker on **5672**, management UI on **15672** |
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
2. Click **Get token** in the auth bar (or open [http://localhost:3000/get-token.html](http://localhost:3000/get-token.html))
3. Sign in with Google → click **Use in ConvertHub** (saves token and returns home)  
   Or copy the token and paste it into the auth bar → **Save**
4. Status should change to `Token set — Authorization: Bearer will be sent`
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
4. The frontend sends `Authorization: Bearer <token>` — set the token in the auth bar first
5. View results and history below

### Tips

- Press **Enter** while focused on an input field to convert
- If you see `401 Unauthorized`, paste a valid Google ID token and click **Save**, then hard refresh: **Ctrl+F5**
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

Obtain a Google ID token first (see [section 7](#7-google-oauth-20--rabbitmq)). Token `aud` must match your `GOOGLE_CLIENT_ID`.

All `/api/**` requests need:

| Header | Value |
|--------|-------|
| `Authorization` | `Bearer YOUR_GOOGLE_ID_TOKEN` |

### Temperature API (port 8081)

Base URL: `http://localhost:8081/api/temperatures`

#### POST convert — success (200)

| Setting | Value |
|---------|-------|
| **Method** | `POST` |
| **URL** | `http://localhost:8081/api/temperatures/convert?value=100&unit=Celsius` |
| **Headers** | `Authorization` = `Bearer YOUR_GOOGLE_ID_TOKEN` |

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
| **Headers** | `Authorization: Bearer YOUR_GOOGLE_ID_TOKEN` |

#### GET history

| Setting | Value |
|---------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8082/api/currency/history` |
| **Headers** | `Authorization: Bearer YOUR_GOOGLE_ID_TOKEN` |

---

## 6. Test with curl

```bash
TOKEN="YOUR_GOOGLE_ID_TOKEN"

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

## 7. Google OAuth 2.0 & RabbitMQ

This project uses **Google as an OAuth 2.0 / OpenID Connect identity provider** in **resource-server** mode:

- You sign in with Google and receive a long **ID token** (JWT, often starts with `eyJ…`).
- Every `/api/**` call sends: `Authorization: Bearer <id_token>`.
- Spring Security validates the token with Google’s public keys (issuer, signature, and Client ID / `aud`).
- **Client Secret is not required** for this flow.
- Do **not** store the ID token in MongoDB or in `.env` — only `GOOGLE_CLIENT_ID` belongs in config. Tokens expire in about **1 hour**.

Both Temperature (**8081**) and Currency (**8082**) use the **same** Bearer token.

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
```

Or set it in PowerShell before `docker compose up`:

```powershell
$env:GOOGLE_CLIENT_ID="xxxx.apps.googleusercontent.com"
docker compose up --build
```

Leave **Client Secret** unused for these APIs.

### How to get a Google ID token (step by step)

1. Start the stack (`docker compose up -d`) and confirm `.env` has your real `GOOGLE_CLIENT_ID`.
2. From the home page, click **Get token** in the auth bar — or open **http://localhost:3000/get-token.html** directly.
3. Click **Sign in with Google**, choose your account, and allow access.
4. Click **Use in ConvertHub** to save the token and go back home automatically.  
   Or click **Copy token** for Postman/curl.
5. Optional check: paste the token at [jwt.io](https://jwt.io). You should see:
   - `iss` = `https://accounts.google.com`
   - `aud` = your Client ID (must match `.env`)

**Do not commit or share ID tokens publicly** — treat them like passwords until they expire.

### Use the token

#### ConvertHub UI

1. Home → **Get token** → Sign in → **Use in ConvertHub**  
   Or paste manually into the auth bar → **Save**
2. Convert as usual on **http://localhost:3000**

#### Postman (important)

Do **not** choose Auth Type **OAuth 2.0** (that asks for Auth URL / Client Secret and often fails with “Invalid protocol for auth URL”).

1. Create request, e.g. `POST http://localhost:8082/api/currency/convert?usdAmount=100`
2. **Authorization** tab → Auth Type → **Bearer Token**
3. Paste **only** the token into Token (Postman adds the `Bearer` prefix)
4. Send — expect **200** JSON
5. Same token works for `http://localhost:8081/api/temperatures/...`

#### curl (PowerShell)

```powershell
$token = "PASTE_FULL_TOKEN_HERE"

curl.exe -X POST "http://localhost:8081/api/temperatures/convert?value=25&unit=celsius" `
  -H "Authorization: Bearer $token"

curl.exe -X POST "http://localhost:8082/api/currency/convert?usdAmount=100" `
  -H "Authorization: Bearer $token"
```

When calls start returning **401**, the token expired — repeat the steps on `/get-token.html`.

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
| AMQP port | `5672` |
| Management UI | http://localhost:15672 |
| Username / password | `guest` / `guest` |
| Exchange | `converthub.exchange` (topic) |
| Temp queue | `temperature.conversion.queue` (routing key `conversion.temperature`) |
| Currency queue | `currency.conversion.queue` (routing key `conversion.currency`) |

Standalone broker:

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management
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

Status should be **Up (healthy)**. Ports: **5672** (AMQP), **15672** (management UI).

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
- Google OAuth resource-server validation on local APIs

### Where production APIs come from

When you visit **https://api.adheesha.dev**, the frontend calls **Cloudflare Workers**, not your local Docker APIs:

| Service | Production URL |
|---------|----------------|
| Currency | `https://currency-converter.vikumkodikara123.workers.dev/api/currency` |
| Temperature | `https://temperature-converter.vikumkodikara123.workers.dev/api/temperatures` |

When you visit **http://localhost:3000**, the frontend calls **localhost:8081** and **8082** instead (with Bearer token).

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

**Check `.env` first:** APIs must not be using `YOUR_GOOGLE_CLIENT_ID`.

```powershell
docker inspect tempconv --format "{{range .Config.Env}}{{println .}}{{end}}" | Select-String "GOOGLE"
```

If wrong, fix `.env` then:

```powershell
docker compose up -d --force-recreate tempconv currencyconvertor
```

**In Postman:** Use Auth Type **Bearer Token** (not OAuth 2.0). Get a fresh token from home → **Get token** or http://localhost:3000/get-token.html.

**In the web UI:** Click **Get token** → **Use in ConvertHub**, or paste in the auth bar → **Save**, then hard refresh (**Ctrl+F5**). Rebuild frontend if UI is stale:

```bash
docker compose build --no-cache frontend
docker compose up -d frontend
```

### Postman “Invalid protocol for auth URL” or OAuth errors

You selected Auth Type **OAuth 2.0**. Switch to **Bearer Token** and paste the Google ID token from `/get-token.html`. You do not need Auth URL, Access Token URL, or Client Secret for these APIs.

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

Ensure ports 3000, 8081, 8082, 27017, 27018, 5672, and 15672 are not in use by other apps.

---

## 10. Quick reference

### All endpoints

| Service | Method | URL | Auth | Port |
|---------|--------|-----|------|------|
| Temperature | GET | `/` | None | 8081 |
| Temperature | GET | `/api/temperatures/safety-check?value=&unit=` | Bearer | 8081 |
| Temperature | GET | `/api/temperatures/history` | Bearer | 8081 |
| Temperature | GET | `/api/temperatures/history/filter?unit=` | Bearer | 8081 |
| Temperature | POST | `/api/temperatures/convert?value=&unit=` | Bearer | 8081 |
| Currency | GET | `/` | None | 8082 |
| Currency | POST | `/api/currency/convert?usdAmount=` | Bearer | 8082 |
| Currency | GET | `/api/currency/history` | Bearer | 8082 |

### MongoDB locations

| Port | Database | Collection | Contents |
|------|----------|------------|----------|
| 27017 | `temp_db` | `conversions` | Temperature history |
| 27018 | `currency_db` | (default) | Currency history |

### Environment placeholders

| Variable | Example |
|----------|---------|
| `GOOGLE_CLIENT_ID` | `xxxx.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | *(not used by resource server)* |
| `RABBITMQ_HOST` | `localhost` or `rabbitmq` |
| `RABBITMQ_PORT` | `5672` |
| `RABBITMQ_USER` / `RABBITMQ_PASS` | `guest` / `guest` |

### Common commands

```bash
# PowerShell
$env:GOOGLE_CLIENT_ID="xxxx.apps.googleusercontent.com"
docker compose up --build          # Start stack
docker compose down                # Stop stack
docker compose ps                  # Check status
docker compose logs -f tempconv    # View temperature API logs
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management
```

---

*Last updated: July 2026*
