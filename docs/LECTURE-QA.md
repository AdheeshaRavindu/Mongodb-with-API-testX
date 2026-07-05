# ConvertHub — Lecture Prep & Q&A

Everything you need to explain this project in a viva, demo, or lecture Q&A.

**Related:** [USAGE-GUIDE.md](USAGE-GUIDE.md) · [PROJECT.md](PROJECT.md) · [README](../README.md)

---

## 30-second elevator pitch

> **ConvertHub** is a microservices app with two Spring Boot APIs (temperature and currency), each backed by its own MongoDB database, plus a web frontend. It converts USD to LKR and temperatures between Celsius, Fahrenheit, and Kelvin, saves every conversion to MongoDB, and implements **Lab 04** (safety-check + filtered history) and **Lab 05** (MongoDB-backed API key auth on temperature convert). The stack runs locally with Docker and the frontend is deployed to Cloudflare Pages.

---

## Must-know facts (memorize these)

| Topic | Answer |
|-------|--------|
| Project name | ConvertHub |
| Backend | Spring Boot 4.0.5, Java 21 |
| Database | MongoDB 7.0 (two separate instances) |
| Frontend | HTML, CSS, vanilla JavaScript |
| UI port | **3000** (not 8081/8082) |
| Temperature API | Port **8081**, database `temp_db` on **27017** |
| Currency API | Port **8082**, database `currency_db` on **27018** |
| Exchange rate | 1 USD = 300 LKR (fixed demo constant) |
| API keys collection | `temp_db.api_keys` on port **27017 only** |
| Valid API key | `SUPER-SECRET-DEV-KEY-123` |
| Inactive test key | `EXPIRED-HACKER-KEY-999` |
| Live demo URL | https://api.adheesha.dev |

---

## Architecture (explain with this)

```
Browser (localhost:3000 or api.adheesha.dev)
        │
        ├──► Temperature API (8081) ──► MongoDB temp_db (27017)
        │         • conversions collection
        │         • api_keys collection
        │
        └──► Currency API (8082) ──► MongoDB currency_db (27018)
                  • currency history
```

**Why two MongoDB instances?** Microservices pattern — each service owns its data. Temperature and currency are independent; one can restart without affecting the other.

**Why microservices?** Separate deploy, separate scaling, separate databases, clear boundaries.

---

## All API endpoints

### Temperature (8081) — base: `/api/temperatures`

| Method | Endpoint | Auth | Lab |
|--------|----------|------|-----|
| POST | `/convert?value=&unit=` | **X-API-KEY** | Lab 05 |
| GET | `/safety-check?value=&unit=` | None | Lab 04 |
| GET | `/history` | None | — |
| GET | `/history/filter?unit=` | None | Lab 04 |

### Currency (8082) — base: `/api/currency`

| Method | Endpoint | Auth |
|--------|----------|------|
| POST | `/convert?usdAmount=` | None |
| GET | `/history` | None |

---

## Conversion formulas

**Temperature:**
- Celsius → Fahrenheit: `(C × 1.8) + 32`
- Fahrenheit → Celsius: `(F − 32) / 1.8`
- Kelvin → Celsius: `K − 273.15`

**Currency:**
- LKR = USD × 300

---

# Predicted lecture questions & answers

---

## General / overview

### Q1: What is this project about?

**A:** ConvertHub is a full-stack converter application. It has two Spring Boot microservices — one for temperature (C/F/K) and one for currency (USD to LKR). Both save conversion history in MongoDB. A web UI lets users convert and view history. Lab 04 added safety warnings and filtered history. Lab 05 added API key authentication on temperature convert, with keys stored in MongoDB.

---

### Q2: Why did you use microservices instead of one monolithic app?

**A:** Each service has a single responsibility — temperature or currency. They use separate MongoDB databases, run on different ports (8081 vs 8082), and can be developed, tested, and deployed independently. This matches real-world patterns where teams own separate services.

---

### Q3: What technologies did you use?

**A:** Spring Boot 4 and Java 21 for REST APIs, Spring Data MongoDB for persistence, MongoDB 7 for storage, HTML/CSS/JavaScript for the frontend, Docker Compose for orchestration, nginx for serving the UI, and Cloudflare Pages for production hosting.

---

### Q4: What is the difference between port 3000, 8081, and 8082?

**A:** Port **3000** is the **web UI** (what users open in the browser). Ports **8081** and **8082** are **REST APIs only** — they return JSON, not a webpage. 8081 is temperature; 8082 is currency.

---

### Q5: What is the live demo URL and what does it deploy?

**A:** https://api.adheesha.dev hosts the **frontend only** on Cloudflare Pages. The live site calls Cloudflare Workers for APIs. The full Spring Boot + MongoDB stack runs locally via Docker — it is not deployed to Cloudflare.

---

## MongoDB

### Q6: Why two MongoDB instances?

**A:** Database-per-service in microservices. Temperature data (`temp_db`) and currency data (`currency_db`) are isolated. Temperature runs on host port **27017**; currency on **27018**.

---

### Q7: What collections exist and what do they store?

**A:**
- **`temp_db.conversions`** — temperature conversion logs (input/output values, units, timestamp)
- **`temp_db.api_keys`** — API keys for Lab 05 (keyValue, owner, active, createdAt)
- **`currency_db`** — currency conversion documents (USD amount, LKR result, rate, timestamp)

---

### Q8: Where are API keys stored?

**A:** In MongoDB: **`temp_db.api_keys`** on port **27017**. Not in environment variables or the currency database. Seeded by `docs/mongo-seed-api-keys.js` via the `mongo-seed` Docker service.

---

### Q9: Why are API keys only on 27017, not 27018?

**A:** Lab 05 applies only to the **temperature convert** endpoint. The currency service has no API key requirement. Keys belong to the temperature microservice’s database.

---

### Q10: What happens if you delete an API key from MongoDB?

**A:** `POST /api/temperatures/convert` returns **401 Unauthorized** with `"Invalid or inactive API key"` (or `"Missing required header X-API-KEY"` if the header is also missing). Other endpoints still work. Re-seed with `mongo-seed-api-keys.js` to restore.

---

### Q11: How are API keys seeded on startup?

**A:** Docker Compose runs a one-shot **`mongo-seed`** container after `mongo-temp` is healthy. It executes `mongosh` with `docs/mongo-seed-api-keys.js`, which upserts two keys into `api_keys`. `tempconv` waits for seed completion before starting.

---

### Q12: Why can't I see `temp_db` in MongoDB Compass?

**A:** On Windows, a **local MongoDB service** may occupy port 27017 instead of Docker. Compass then shows `test` instead of `temp_db`. Fix: `net stop MongoDB` (Admin PowerShell), then refresh Compass.

---

## Lab 04 — Safety check & filtered history

### Q13: What did Lab 04 add?

**A:** Two features:
1. **GET `/safety-check`** — returns plain-text warnings about temperature safety
2. **GET `/history/filter?unit=`** — returns conversion history filtered by input unit (e.g. only Celsius)

---

### Q14: Explain the safety-check logic.

**A:** The service normalizes the unit, converts to Fahrenheit and Celsius internally, then:
1. If **Fahrenheit > 100°F** → `"Warning: … is dangerously HOT! Stay hydrated."`
2. Else if **Celsius is between 15°C and 30°C** → `"The temperature is comfortable and safe."`
3. Else → `"Warning: … is outside the comfortable range. Use caution."`

Constants: `HOT_FAHRENHEIT_THRESHOLD = 100`, comfort range `15–30°C`.

---

### Q15: How does filtered history work?

**A:** `TemperatureRepository.findByInputUnit(String)` queries MongoDB for documents where `inputUnit` matches (e.g. `"Celsius"`). The controller calls `GET /history/filter?unit=celsius`, normalizes the unit parameter, and returns the filtered list.

---

### Q16: Does safety-check require an API key?

**A:** **No.** Only `POST /convert` requires authentication. Safety-check, history, and filtered history are public.

---

## Lab 05 — API key authentication

### Q17: What did Lab 05 add?

**A:** MongoDB-backed API key validation on **`POST /api/temperatures/convert` only**. Clients must send header **`X-API-KEY`**. The server looks up the key in `api_keys`, checks it exists and `active: true`, otherwise returns 401.

---

### Q18: Walk through the API key validation flow.

**A:**
1. Client sends `POST /convert` with header `X-API-KEY: <key>`
2. `TemperatureController` reads header via `@RequestHeader("X-API-KEY")`
3. `TemperatureService.validateApiKey()` runs:
   - Empty/missing header → `UnauthorizedApiKeyException` → 401
   - Key not in DB → 401 `"Invalid or inactive API key"`
   - Key found but `active: false` → 401
   - Key valid and active → proceed to convert and save
4. `GlobalExceptionHandler` maps the exception to HTTP 401 with plain-text body

---

### Q19: What are the two seeded keys and why both?

**A:**
| Key | Active | Purpose |
|-----|--------|---------|
| `SUPER-SECRET-DEV-KEY-123` | true | Valid key — successful convert |
| `EXPIRED-HACKER-KEY-999` | false | Demo inactive key — tests 401 rejection |

---

### Q20: Why use `@RequestHeader(required = false)` instead of `required = true`?

**A:** With `required = false`, Spring does not auto-reject missing headers. Custom validation in `validateApiKey()` returns a clear message: `"Missing required header X-API-KEY"` instead of a generic Spring error.

---

### Q21: What HTTP status codes does the API return for auth errors?

**A:** **401 Unauthorized** for missing, invalid, or inactive keys. **400 Bad Request** for bad input (e.g. unsupported unit) via `IllegalArgumentException`.

---

### Q22: Should API keys be in a `.env` file or git?

**A:** For this **lab/demo**, keys in `mongo-seed-api-keys.js` are intentional dev values — fine in git. For **production**, real keys should be in secrets/env, never committed. Note: the frontend also hardcodes the dev key in `app.js`, so it is visible in the browser anyway.

---

### Q23: Does the currency API use API keys?

**A:** **No.** Only temperature convert is protected. Currency convert and history are open.

---

## Spring Boot / code structure

### Q24: Explain the layered architecture.

**A:**
- **Controller** — HTTP endpoints, reads params/headers, returns JSON
- **Service** — business logic (convert, safety check, validate API key)
- **Repository** — MongoDB access via Spring Data (`MongoRepository`)
- **Model** — documents mapped with `@Document` (e.g. `TemperatureLog`, `ApiKey`)
- **Exception** — custom exceptions + `@RestControllerAdvice` for global error handling

---

### Q25: What is `@CrossOrigin(origins = "*")` for?

**A:** Enables CORS so the frontend (different port/origin) can call the API from the browser without being blocked.

---

### Q26: What is `@Document(collection = "api_keys")`?

**A:** Spring Data MongoDB annotation. Maps the `ApiKey` Java class to the `api_keys` collection in MongoDB.

---

### Q27: How does Spring Boot connect to MongoDB?

**A:** Via `application.yaml`:
```yaml
spring:
  mongodb:
    uri: ${MONGODB_URI:mongodb://localhost:27017/temp_db}
```
Docker sets `MONGODB_URI` environment variable. Spring Boot 4 uses `spring.mongodb.uri` (not `spring.data.mongodb.uri`).

---

## Frontend

### Q28: How does the frontend know which API to call?

**A:** `app.js` checks `window.location.hostname`. If `localhost` or `127.0.0.1`, it uses `http://localhost:8081` and `8082`. Otherwise it uses Cloudflare Worker URLs for production.

---

### Q29: How does the frontend send the API key?

**A:** On temperature convert, `fetch()` includes:
```javascript
headers: { 'X-API-KEY': 'SUPER-SECRET-DEV-KEY-123' }
```
Currency convert does not send a key.

---

### Q30: Why did you get `Missing required header X-API-KEY` in Postman?

**A:** Lab 05 requires the header on `POST /convert`. Postman request had query params but no `X-API-KEY` header. Fix: Headers tab → add `X-API-KEY: SUPER-SECRET-DEV-KEY-123`.

---

## Docker

### Q31: What does `docker compose up --build` start?

**A:** Six services: two MongoDB instances, mongo-seed (one-shot), tempconv, currencyconvertor, and frontend (nginx on port 3000). Persistent volumes keep data between restarts.

---

### Q32: Why does `tempconv` depend on `mongo-seed`?

**A:** So API keys exist in MongoDB **before** the temperature API accepts convert requests. Without seed, valid key lookups would fail.

---

### Q33: What is the frontend Docker container?

**A:** nginx Alpine serving static files (`index.html`, `style.css`, `app.js`) on port 80, mapped to host port 3000.

---

## Deployment

### Q34: What gets deployed to Cloudflare?

**A:** Only the **`frontend/`** folder — static HTML/CSS/JS. Not Spring Boot, not MongoDB, not API keys.

---

### Q35: Does Lab 05 work on the live Cloudflare site?

**A:** **Partially.** The frontend sends `X-API-KEY`, but production calls **Cloudflare Workers**, not your local Spring Boot. MongoDB key validation in Lab 05 runs on **local Docker only** unless Workers are updated to match.

---

### Q36: How do you deploy the frontend?

**A:**
```bash
npx wrangler login
npx wrangler pages deploy frontend --project-name=converthub-mongodb-api
```

---

## Demo / practical questions

### Q37: Demo temperature convert in Postman.

**A:**
1. Method: **POST**
2. URL: `http://localhost:8081/api/temperatures/convert?value=100&unit=Celsius`
3. Header: `X-API-KEY: SUPER-SECRET-DEV-KEY-123`
4. Expected: **200** JSON with `outputTemperature: 212`, `outputUnit: Fahrenheit`

---

### Q38: Demo currency convert in Postman.

**A:**
1. Method: **POST**
2. URL: `http://localhost:8082/api/currency/convert?usdAmount=100`
3. No headers needed
4. Expected: **200** JSON with `outputAmount: 30000`, `outputCurrency: LKR`

---

### Q39: Demo Lab 04 safety-check.

**A:**
```
GET http://localhost:8081/api/temperatures/safety-check?value=102&unit=F
→ "Warning: 102.0°F is dangerously HOT! Stay hydrated."

GET http://localhost:8081/api/temperatures/safety-check?value=21&unit=C
→ "The temperature is comfortable and safe."
```

---

### Q40: Demo Lab 05 — show 401 vs 200.

**A:**
1. POST convert **without** header → **401** `Missing required header X-API-KEY`
2. POST with `EXPIRED-HACKER-KEY-999` → **401** `Invalid or inactive API key`
3. POST with `SUPER-SECRET-DEV-KEY-123` → **200** success

---

## Tricky / follow-up questions

### Q41: Is this secure for production?

**A:** **No, not as-is.** The API key is hardcoded in frontend JavaScript (visible to anyone). Keys are demo values in git. For production you would: keep secrets server-side only, use HTTPS, rotate keys, rate-limit, and never expose keys in client code for sensitive operations.

---

### Q42: Why not protect all endpoints with API keys?

**A:** Lab 05 spec protects **convert only** — the write operation that creates data. Read endpoints (history, safety-check) stay public for easier testing and demo.

---

### Q43: What design pattern is used for MongoDB access?

**A:** **Repository pattern** — `TemperatureRepository` and `ApiKeyRepository` extend `MongoRepository`, giving CRUD and custom query methods like `findByInputUnit` and `findByKeyValue`.

---

### Q44: What happens when you convert Celsius to Fahrenheit?

**A:** Input stored as `inputUnit: Celsius`, formula `(value × 1.8) + 32`, output saved as `outputUnit: Fahrenheit`, document persisted in `conversions` collection.

---

### Q45: Can you filter history by Fahrenheit?

**A:** Yes: `GET /api/temperatures/history/filter?unit=fahrenheit` — unit is normalized (accepts `f`, `Fahrenheit`, etc.) and matches stored `inputUnit` field.

---

### Q46: What is the difference between `/history` and `/history/filter`?

**A:** `/history` returns **all** conversion logs. `/history/filter?unit=` returns only logs where the **input unit** matches the parameter.

---

### Q47: Why use plain text for safety-check instead of JSON?

**A:** Lab 04 spec returns human-readable warnings as plain text (`produces = MediaType.TEXT_PLAIN_VALUE`). Easier to read in browser/Postman for demo purposes.

---

### Q48: What if someone sends a wrong unit like `unit=xyz`?

**A:** `IllegalArgumentException` → **400 Bad Request** with message `"Unsupported unit. Use Celsius, Fahrenheit, or Kelvin."`

---

## Quick cheat sheet (last-minute review)

```
UI:        http://localhost:3000
Temp API:  http://localhost:8081/api/temperatures
Currency:  http://localhost:8082/api/currency

MongoDB:   27017 = temp_db (api_keys + conversions)
           27018 = currency_db

Lab 04:    safety-check, history/filter
Lab 05:    X-API-KEY on POST convert only

Valid key: SUPER-SECRET-DEV-KEY-123
Bad key:   EXPIRED-HACKER-KEY-999

Docker:    docker compose up --build
Stop:      docker compose down
```

---

*Good luck in your lecture.*
