# ConvertHub — Temperature & Currency Converter

Spring Boot microservices + MongoDB for USD/LKR currency and temperature conversion, secured with **Google OAuth 2.0** (resource server) and emitting **RabbitMQ** conversion events.

**Live demo:** [https://api.adheesha.dev](https://api.adheesha.dev)

**Full usage guide:** [docs/USAGE-GUIDE.md](docs/USAGE-GUIDE.md)

![ConvertHub demo](docs/demo.png)

## Features

- Currency conversion (USD → LKR) with MongoDB history
- Temperature conversion (Celsius, Fahrenheit, Kelvin) with history
- Safety check endpoint for heat warnings (Lab 04)
- Filtered temperature history by input unit (Lab 04)
- Google OAuth 2.0 (JWT / ID token) on all `/api/**` endpoints
- RabbitMQ events after every successful temperature and currency conversion
- Dockerized full stack (frontend + 2 APIs + 2 MongoDB instances + RabbitMQ)

## Architecture

```
Frontend (3000) ──┬── Temperature API (8081) ── MongoDB temp_db (27017)
                  │         │
                  │         └──► RabbitMQ (5672) ── temperature.conversion.queue
                  └── Currency API (8082)   ── MongoDB currency_db (27018)
                            └──► RabbitMQ (5672) ── currency.conversion.queue
```

| Service | Port | Notes |
|---------|------|-------|
| Frontend UI | 3000 | Open this in the browser |
| Temperature API | 8081 | REST only — not the UI |
| Currency API | 8082 | REST only — not the UI |
| MongoDB (temp) | 27017 | `temp_db` |
| MongoDB (currency) | 27018 | `currency_db` |
| RabbitMQ | 5672 / 15672 | AMQP + management UI |

## Quick start (Docker)

Set your Google OAuth **Client ID** (from [Google Cloud Console](https://console.cloud.google.com/apis/credentials)):

```bash
# Windows PowerShell
$env:GOOGLE_CLIENT_ID="YOUR_CLIENT_ID.apps.googleusercontent.com"
docker compose up --build
```

Or run RabbitMQ alone:

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management
```

Open **http://localhost:3000** for the UI. Paste a Google ID token in the auth bar.

Stop:

```bash
docker compose down
```

## Project layout

```
Mongodb-with-API-testX/
├── tempconv/           # Temperature microservice (8081)
├── currencyconvertor/  # Currency microservice (8082)
├── frontend/           # Web UI
├── docs/               # Guides, seed script
└── docker-compose.yml
```

## API reference

All `/api/**` routes require: `Authorization: Bearer <Google_ID_Token>`

`GET /` on each service remains public (service info).

### Temperature (`8081`)

| Method | Endpoint | Auth |
|--------|----------|------|
| GET | `/api/temperatures/safety-check?value=&unit=` | Bearer |
| GET | `/api/temperatures/history` | Bearer |
| GET | `/api/temperatures/history/filter?unit=` | Bearer |
| POST | `/api/temperatures/convert?value=&unit=` | Bearer |

### Currency (`8082`)

| Method | Endpoint | Auth |
|--------|----------|------|
| POST | `/api/currency/convert?usdAmount=` | Bearer |
| GET | `/api/currency/history` | Bearer |

### Example curl

```bash
# Missing token → 401
curl -X POST "http://localhost:8081/api/temperatures/convert?value=25&unit=celsius"

# Valid Google ID token → 200
curl -X POST "http://localhost:8081/api/temperatures/convert?value=25&unit=celsius" \
  -H "Authorization: Bearer YOUR_GOOGLE_ID_TOKEN"

curl -X POST "http://localhost:8082/api/currency/convert?usdAmount=100" \
  -H "Authorization: Bearer YOUR_GOOGLE_ID_TOKEN"
```

### RabbitMQ

- Exchange: `converthub.exchange` (topic)
- Temp routing key / queue: `conversion.temperature` → `temperature.conversion.queue`
- Currency routing key / queue: `conversion.currency` → `currency.conversion.queue`
- Management UI: http://localhost:15672 (`guest` / `guest`)

After a successful convert, check service logs for `Published …` and `Received … conversion event`.

## Local run (without Docker)

**Prerequisites:** Java 21, Maven, MongoDB 6+, RabbitMQ

1. Start MongoDB on ports `27017` and `27018`
2. Start RabbitMQ (see Docker command above)
3. Set `GOOGLE_CLIENT_ID` and run:
   ```bash
   cd tempconv && ./mvnw spring-boot:run
   cd currencyconvertor && ./mvnw spring-boot:run
   ```
4. Serve `frontend/` (or use nginx / Live Server) on port 3000

## Environment variables

| Variable | Purpose | Default |
|----------|---------|---------|
| `GOOGLE_CLIENT_ID` | Google OAuth client ID (JWT `aud`) | `YOUR_GOOGLE_CLIENT_ID` |
| `GOOGLE_CLIENT_SECRET` | Not used by resource-server JWT validation | — |
| `RABBITMQ_HOST` | Broker host | `localhost` |
| `RABBITMQ_PORT` | Broker port | `5672` |
| `RABBITMQ_USER` / `RABBITMQ_PASS` | Credentials | `guest` / `guest` |
| `MONGODB_URI` | Per-service Mongo URI | see `application.yaml` |
