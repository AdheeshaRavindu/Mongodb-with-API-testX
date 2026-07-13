# ConvertHub — Docker Guide

Simple Docker commands for this project, ordered **most important → least important**.

Run all commands from the project root:

```text
D:\Github\Projects\Mongodb-with-API-testX
```

**Related docs:** [README](../README.md) · [USAGE-GUIDE](USAGE-GUIDE.md)

## What's dockerized (full stack)

When you run `docker compose up -d`, **11 containers** start:

```text
docker compose up -d
        │
        ├── converthub-frontend  (3000)  ← UI
        ├── api-gateway            (8080)  ← API entry point
        ├── tempconv               (internal 8081)  ← temp + auth
        ├── currencyconvertor      (internal 8082)  ← currency
        ├── redis                  (internal 6379)  ← rate limiting
        ├── mongo-temp             (27017) ← DB + users
        ├── mongo-currency         (27018) ← DB
        ├── rabbitmq               (56720, 15672)
        ├── mongo-seed             (runs once)
        └── mongo-currency-seed    (runs once)
```

**Commands (run from project root):**

| When | Command |
|------|---------|
| **Normal start** (no code changes) | `docker compose up -d` |
| **Build + start** (after code changes) | `docker compose up --build -d` |
| **Stop entire stack** | `docker compose down` |
| **Stop MongoDB only** | `docker compose stop mongo-temp mongo-currency` |
| **Start MongoDB again** | `docker compose start mongo-temp mongo-currency` |
| **Stop Windows MongoDB service** (frees port 27017) | `net stop MongoDB` (Admin PowerShell) |

```powershell
# Normal start
docker compose up -d

# Build + start (first time or after code changes)
docker compose up --build -d

# Stop everything
docker compose down

# Stop MongoDB containers only (Docker)
docker compose stop mongo-temp mongo-currency

# Stop local Windows MongoDB service (if it blocks port 27017)
# Run PowerShell as Administrator:
net stop MongoDB

# Start local Windows MongoDB again (optional)
net start MongoDB
```

### Your app (built from your code)

| Container | What it is | Port |
|-----------|------------|------|
| `converthub-frontend` | Web UI (nginx) | **3000** |
| `api-gateway` | API Gateway (routing, JWT, rate limit) | **8080** |
| `tempconv` | Temperature API + auth (internal) | internal **8081** |
| `currencyconvertor` | Currency API (internal) | internal **8082** |
| `redis` | Rate limiting backend | internal **6379** |

### Supporting services

| Container | What it is | Port |
|-----------|------------|------|
| `mongo-temp` | MongoDB (temperature + users) | **27017** |
| `mongo-currency` | MongoDB (currency history) | **27018** |
| `rabbitmq` | Message broker | **56720** (AMQP), **15672** (dashboard) |

### One-shot jobs (exit after done — normal)

| Container | Role |
|-----------|------|
| `mongo-seed` | Seeds `temp_db` |
| `mongo-currency-seed` | Seeds `currency_db` |

### Not separate containers

- **Auth logic** → inside `tempconv`
- **Google Sign-In** → browser (Google)
- **JWT after login** → browser localStorage

---

## Quick map (what Docker runs)

| Priority | What you open | Port |
|----------|---------------|------|
| **1** | Web UI | **http://localhost:3000** |
| **2** | API Gateway (Postman / API calls) | **http://localhost:8080** |
| 3 | RabbitMQ dashboard | http://localhost:15672 (`guest` / `guest`) |
| 4 | MongoDB (Compass) | `localhost:27017`, `localhost:27018` |

| Container | Role |
|-----------|------|
| `converthub-frontend` | Web UI (nginx) |
| `tempconv` | Temperature API + login (`POST /auth/google`) |
| `currencyconvertor` | Currency API |
| `mongo-temp` | Temperature database |
| `mongo-currency` | Currency database |
| `rabbitmq` | Message broker |
| `mongo-seed` / `mongo-currency-seed` | One-shot seed jobs (exit after done) |

---

# Level 1 â€” Essential (use these first)

These are the commands you need on day one.

## 1.1 Prerequisites

1. Install **Docker Desktop** and start it (whale icon in system tray must be running).
2. Open a terminal in the project folder.

Check Docker works:

```powershell
docker --version
docker compose version
```

## 1.2 One-time setup: create `.env`

Do this **once per machine** before the first run.

```powershell
copy .env.example .env
```

Edit `.env` and set real values:

```env
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
JWT_SECRET=your-long-random-secret-at-least-32-characters
JWT_EXPIRATION_HOURS=24
```

> **Why it matters:** Wrong `GOOGLE_CLIENT_ID` or `JWT_SECRET` causes **401** on login or API calls.

## 1.3 Start the full stack (most used command)

**First time** or after pulling big changes:

```powershell
docker compose up --build -d
```

**Normal start** (no code changes):

```powershell
docker compose up -d
```

| Flag | Meaning |
|------|---------|
| `up` | Start all services |
| `--build` | Rebuild images from code |
| `-d` | Run in background (detached) |

## 1.4 Check everything is running

```powershell
docker compose ps
```

**Healthy stack looks like:**

- `converthub-frontend`, `tempconv`, `currencyconvertor`, `mongo-temp`, `mongo-currency`, `rabbitmq` â†’ **Up**
- `mongo-seed`, `mongo-currency-seed` â†’ **Exited (0)** â€” this is normal

## 1.5 Open the app

1. Browser â†’ **http://localhost:3000**
2. Click **Sign in with Google** in the auth bar
3. Use Currency / Temperature converters

> Ports **8081** and **8082** are APIs only â€” not the web UI.

## 1.6 Stop the stack

```powershell
docker compose down
```

Stops containers but **keeps database data** (volumes are preserved).

---

# Level 2 â€” Important (after code or config changes)

Use these when something changed and a simple `up -d` is not enough.

## 2.1 You changed Java/backend code

Rebuild and restart API services:

```powershell
docker compose up --build -d tempconv currencyconvertor
```

## 2.2 You changed frontend files (`frontend/`)

Rebuild only the frontend:

```powershell
docker compose up --build -d frontend
```

If the UI still looks old, force a clean frontend build:

```powershell
docker compose build --no-cache frontend
docker compose up -d frontend
```

## 2.3 You changed `.env` (GOOGLE_CLIENT_ID, JWT_SECRET, etc.)

Recreate API containers so they pick up new env vars â€” **no full rebuild needed**:

```powershell
docker compose up -d --force-recreate tempconv currencyconvertor
```

## 2.4 You changed `docker-compose.yml`

Recreate affected services:

```powershell
docker compose up -d --force-recreate
```

Or rebuild everything:

```powershell
docker compose up --build -d
```

## 2.5 View logs (when something fails)

**Follow live logs** (Ctrl+C to stop watching):

```powershell
docker compose logs -f tempconv
docker compose logs -f currencyconvertor
docker compose logs -f converthub-frontend
docker compose logs -f rabbitmq
```

**Last 100 lines only:**

```powershell
docker compose logs --tail=100 tempconv
```

**All services at once:**

```powershell
docker compose logs -f
```

## 2.6 Restart one service (quick fix)

```powershell
docker compose restart tempconv
docker compose restart currencyconvertor
docker compose restart converthub-frontend
docker compose restart rabbitmq
```

---

# Level 3 â€” Troubleshooting

Use when the stack won't start, APIs return errors, or ports clash.

## 3.1 Docker Desktop not running

**Error example:**

```text
open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified
```

**Fix:** Start Docker Desktop, wait until it is ready, then run `docker compose up -d` again.

## 3.2 API returns 401 Unauthorized

**Step 1 â€” Check env inside container:**

```powershell
docker inspect tempconv --format "{{range .Config.Env}}{{println .}}{{end}}" | Select-String "GOOGLE|JWT"
docker inspect currencyconvertor --format "{{range .Config.Env}}{{println .}}{{end}}" | Select-String "JWT"
```

**Step 2 â€” Fix `.env`, then recreate APIs:**

```powershell
docker compose up -d --force-recreate tempconv currencyconvertor
```

**Step 3 — Sign in again** using the Google button in the home page auth bar (or http://localhost:3000/get-token.html)

## 3.3 API container keeps restarting

```powershell
docker compose ps
docker compose logs --tail=50 tempconv
docker compose logs --tail=50 currencyconvertor
```

Common causes: MongoDB or RabbitMQ not healthy yet. Wait 30 seconds and check `docker compose ps` again.

## 3.4 Port already in use

**Error example:** `Bind for 0.0.0.0:8081 failed: port is already allocated`

**Check what uses the port (PowerShell):**

```powershell
netstat -ano | findstr :3000
netstat -ano | findstr :8081
netstat -ano | findstr :8082
netstat -ano | findstr :27017
```

**Ports this project needs:**

| Port | Service |
|------|---------|
| 3000 | Frontend |
| 8080 | API Gateway |
| 3000 | Frontend |
| 27017 | MongoDB (temp) |
| 27018 | MongoDB (currency) |
| 56720 | RabbitMQ AMQP (host; container still 5672) |
| 15672 | RabbitMQ UI |

Stop the conflicting app, or stop this stack with `docker compose down`.

## 3.4a Windows: port bind forbidden by access permissions

**Error example:**

```text
listen tcp 0.0.0.0:5672: bind: An attempt was made to access a socket in a way forbidden by its access permissions.
```

This usually means **no app is using the port**. Windows has **reserved** it (common with Hyper-V, WSL2, or Docker Desktop).

**Check excluded port ranges (PowerShell):**

```powershell
netsh interface ipv4 show excludedportrange protocol=tcp
```

If **5672** falls inside a range (for example `5651-5750`), Docker cannot bind to it.

**Fix used by this repo:** RabbitMQ AMQP is published on host port **56720** instead of **5672** (`56720:5672` in `docker-compose.yml`). Containers still talk to `rabbitmq:5672` on the Docker network. The frontend and APIs are unchanged. Use **56720** only when connecting from Windows tools outside Compose.

**Optional (advanced):** Free the range by adjusting Hyper-V / `winnat` port exclusions and rebooting. Changing the compose mapping is simpler.

## 3.5 Can't see `temp_db` in MongoDB Compass

Local Windows MongoDB may be using port 27017.

```powershell
net stop MongoDB
```

Then refresh Compass on `mongodb://localhost:27017`.

## 3.6 Frontend shows old UI after edits

```powershell
docker compose build --no-cache frontend
docker compose up -d frontend
```

Hard refresh browser: **Ctrl+F5**

## 3.7 RabbitMQ not receiving messages

1. Open http://localhost:15672 (`guest` / `guest`)
2. Check queues: `temperature.conversion.queue`, `currency.conversion.queue`
3. Perform a convert with a signed-in user
4. Watch API logs:

```powershell
docker compose logs -f tempconv
```

---

# Level 4 â€” Maintenance & reset

Use carefully â€” some commands delete data.

## 4.1 Full rebuild (clean images)

When builds act strange or dependencies changed:

```powershell
docker compose build --no-cache
docker compose up -d
```

Rebuild one service only:

```powershell
docker compose build --no-cache tempconv
docker compose up -d tempconv
```

## 4.2 Stop and remove containers (keep data)

```powershell
docker compose down
```

## 4.3 Reset databases (delete all conversion history)

This removes MongoDB volumes â€” **all stored conversions are lost**.

```powershell
docker compose down -v
docker compose up --build -d
```

The seed containers will run again on next start.

## 4.4 Re-seed API keys manually (temperature DB)

```powershell
docker run --rm --network mongodb-with-api-testx_default -v "${PWD}/docs/mongo-seed-api-keys.js:/seed.js:ro" mongo:7.0 mongosh mongodb://mongo-temp:27017/temp_db --file /seed.js
```

> Network name may differ. List networks with `docker network ls` if this fails.

## 4.5 Remove unused Docker clutter (optional)

Frees disk space on your machine â€” affects other Docker projects too.

```powershell
docker system prune
```

Aggressive cleanup (removes unused images):

```powershell
docker system prune -a
```

---

# Level 5 â€” Reference (less frequent)

## 5.1 Environment variables (from `.env`)

| Variable | Used by | Purpose |
|----------|---------|---------|
| `GOOGLE_CLIENT_ID` | `tempconv` | Verify Google login |
| `JWT_SECRET` | Both APIs | Sign/validate app JWT (**must match**) |
| `JWT_EXPIRATION_HOURS` | Both APIs | Token lifetime (default 24h) |

Compose injects these into containers automatically from `.env`.

## 5.2 Run only some services

```powershell
docker compose up -d rabbitmq mongo-temp mongo-currency
docker compose up -d tempconv currencyconvertor
docker compose up -d frontend
```

## 5.3 Shell into a running container

```powershell
docker exec -it tempconv sh
docker exec -it mongo-temp mongosh
docker exec -it rabbitmq bash
```

## 5.4 Inspect a single container

```powershell
docker inspect tempconv
docker port tempconv
docker stats tempconv currencyconvertor
```

## 5.5 RabbitMQ without full stack

```powershell
docker run -d --name rabbitmq -p 56720:5672 -p 15672:15672 rabbitmq:3.13-management
```

Stop/remove standalone RabbitMQ:

```powershell
docker stop rabbitmq
docker rm rabbitmq
```

## 5.6 Image and container names

| Compose service | Image / build | Container name |
|-----------------|---------------|----------------|
| `frontend` | `./frontend` | `converthub-frontend` |
| `tempconv` | `./tempconv` | `tempconv` |
| `currencyconvertor` | `./currencyconvertor` | `currencyconvertor` |
| `mongo-temp` | `mongo:7.0` | `mongo-temp` |
| `mongo-currency` | `mongo:7.0` | `mongo-currency` |
| `rabbitmq` | `rabbitmq:3.13-management` | `rabbitmq` |

---

# Cheat sheet (copy-paste)

Ordered by how often you need them:

```powershell
# 1. START (most important)
docker compose up -d

# 2. FIRST TIME / BIG CODE CHANGES
docker compose up --build -d

# 3. CHECK STATUS
docker compose ps

# 4. OPEN APP
# http://localhost:3000

# 5. STOP
docker compose down

# 6. CHANGED .env
docker compose up -d --force-recreate tempconv currencyconvertor

# 7. CHANGED BACKEND CODE
docker compose up --build -d tempconv currencyconvertor

# 8. CHANGED FRONTEND CODE
docker compose up --build -d frontend

# 9. VIEW LOGS
docker compose logs -f tempconv

# 10. FULL RESET (deletes DB data)
docker compose down -v
docker compose up --build -d
```

---

# Typical workflows

## Daily development

```powershell
docker compose up -d
docker compose ps
# work in browser at http://localhost:3000
docker compose down
```

## After editing Spring Boot code

```powershell
docker compose up --build -d tempconv currencyconvertor
docker compose logs -f tempconv
```

## After editing frontend

```powershell
docker compose up --build -d frontend
```

## After changing secrets in `.env`

```powershell
docker compose up -d --force-recreate tempconv currencyconvertor
```

## Fresh start (wipe databases)

```powershell
docker compose down -v
docker compose up --build -d
```

---

*Last updated: July 2026*

