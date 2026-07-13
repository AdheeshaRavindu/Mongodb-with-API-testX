# ConvertHub — Temperature & Currency Converter



Spring Boot microservices + MongoDB for USD/LKR currency and temperature conversion, secured with **Google Sign-In** (login) and **application-issued JWTs** (API access), plus **RabbitMQ** conversion events.



**Live demo:** [https://api.adheesha.dev](https://api.adheesha.dev)



**Full usage guide:** [docs/USAGE-GUIDE.md](docs/USAGE-GUIDE.md) · **Docker guide:** [docs/DOCKER-GUIDE.md](docs/DOCKER-GUIDE.md)



![ConvertHub demo](docs/demo.png)



## Features



- Currency conversion (USD → LKR) with MongoDB history

- Temperature conversion (Celsius, Fahrenheit, Kelvin) with history

- Safety check endpoint for heat warnings (Lab 04)

- Filtered temperature history by input unit (Lab 04)

- Google Sign-In on the frontend; backend issues application JWTs for `/api/**`

- RabbitMQ events after every successful temperature and currency conversion

- Dockerized full stack (frontend + API gateway + 2 APIs + 2 MongoDB instances + RabbitMQ + Redis)



## Architecture



```

Frontend (3000) ── Google Sign-In ──► API Gateway (8080)
                  │                      │
                  │                      ├── POST /auth/google ──► tempconv
                  │                      ├── /api/temperatures/** ──► tempconv
                  │                      └── /api/currency/** ──► currencyconvertor
                  │                      └── JWT validation + rate limiting (Redis)
                  ├── tempconv ── MongoDB temp_db (27017)
                  │         └──► RabbitMQ ── temperature.conversion.queue
                  └── currencyconvertor ── MongoDB currency_db (27018)
                            └──► RabbitMQ ── currency.conversion.queue

```



| Service | Port | Notes |

|---------|------|-------|

| Frontend UI | 3000 | Open this in the browser |

| **API Gateway** | **8080** | **Single entry point for all API calls** |

| Temperature API | internal 8081 | Routed via gateway only |

| Currency API | internal 8082 | Routed via gateway only |

| Redis | internal 6379 | Gateway rate limiting |

| MongoDB (temp) | 27017 | `temp_db` |

| MongoDB (currency) | 27018 | `currency_db` |

| RabbitMQ | 56720 / 15672 | AMQP (host) + management UI |



## Quick start (Docker)



### One-time: create `.env`



```powershell

copy .env.example .env

# Edit .env — set GOOGLE_CLIENT_ID and JWT_SECRET

```



### Start



```powershell

docker compose up -d

```



Open **http://localhost:3000** → click **Sign in** → sign in with Google → you are redirected home with an application JWT stored in the browser.



After code changes: `docker compose up --build -d`  

After `.env` changes: `docker compose up -d --force-recreate tempconv currencyconvertor`



Or run RabbitMQ alone:



```bash

docker run -d --name rabbitmq -p 56720:5672 -p 15672:15672 rabbitmq:3.13-management

```



Open **http://localhost:3000** for the UI. Use **Sign in** on the home page to authenticate.



Stop:



```bash

docker compose down

```



## Project layout



```

Mongodb-with-API-testX/

├── .env.example        # Copy to .env once (not in git)

├── api-gateway/        # API Gateway (8080)
├── tempconv/           # Temperature microservice (internal)

├── currencyconvertor/  # Currency microservice (internal)

├── frontend/           # Web UI + get-token.html (sign-in page)

├── docs/               # Guides, seed script

└── docker-compose.yml

```



## API reference



### Authentication



| Method | Endpoint | Auth | Service |

|--------|----------|------|---------|

| POST | `/auth/google` | None (body: `{ "idToken": "..." }`) | 8081 |



Response:



```json

{

  "token": "<application_jwt>",

  "user": { "googleId": "...", "email": "...", "name": "...", "picture": "..." }

}

```



All `/api/**` routes require: `Authorization: Bearer <application_jwt>`



`GET /` on each service remains public (service info).



### Temperature (`8081`)



| Method | Endpoint | Auth |

|--------|----------|------|

| GET | `/api/temperatures/safety-check?value=&unit=` | Bearer (app JWT) |

| GET | `/api/temperatures/history` | Bearer (app JWT) |

| GET | `/api/temperatures/history/filter?unit=` | Bearer (app JWT) |

| POST | `/api/temperatures/convert?value=&unit=` | Bearer (app JWT) |



### Currency (via gateway)



| Method | Endpoint | Auth |

|--------|----------|------|

| POST | `/api/currency/convert?usdAmount=` | Bearer (app JWT) |

| GET | `/api/currency/history` | Bearer (app JWT) |



### Example curl



```bash

# 1. Exchange Google ID token for application JWT (get idToken from /get-token.html sign-in)

curl -X POST "http://localhost:8080/auth/google" \

  -H "Content-Type: application/json" \

  -d '{"idToken":"YOUR_GOOGLE_ID_TOKEN"}'



# 2. Use returned token on protected APIs

TOKEN="YOUR_APPLICATION_JWT"



curl -X POST "http://localhost:8080/api/temperatures/convert?value=25&unit=celsius" \

  -H "Authorization: Bearer $TOKEN"



curl -X POST "http://localhost:8080/api/currency/convert?usdAmount=100" \

  -H "Authorization: Bearer $TOKEN"

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

3. Set `GOOGLE_CLIENT_ID`, `JWT_SECRET` (same value on both APIs), and run:

   ```bash

   cd tempconv && ./mvnw spring-boot:run

   cd currencyconvertor && ./mvnw spring-boot:run

   ```

4. Serve `frontend/` (or use nginx / Live Server) on port 3000



## Environment variables



| Variable | Purpose | Default |

|----------|---------|---------|

| `GOOGLE_CLIENT_ID` | Verify Google ID token at login (tempconv only) | `YOUR_GOOGLE_CLIENT_ID` |

| `JWT_SECRET` | Sign/validate application JWT (must match on both APIs) | `change-me-to-a-long-random-secret-key` |

| `JWT_EXPIRATION_HOURS` | Application token TTL | `24` |

| `RABBITMQ_HOST` | Broker host | `localhost` |

| `RABBITMQ_PORT` | Broker port (`5672` in Compose; `56720` from host) | `5672` |

| `RABBITMQ_USER` / `RABBITMQ_PASS` | Credentials | `guest` / `guest` |

| `MONGODB_URI` | Per-service Mongo URI | see `application.yaml` |

