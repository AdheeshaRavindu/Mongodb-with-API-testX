# ConvertHub — Temperature & Currency Converter

Spring Boot microservices + MongoDB for USD/LKR currency and temperature conversion, with a clean organic web UI.

**Live demo:** [https://api.adheesha.dev](https://api.adheesha.dev)

![ConvertHub demo](docs/demo.png)

## Features

- Currency conversion (USD → LKR) with history
- Temperature conversion (Celsius, Fahrenheit, Kelvin) with history
- Two independent Spring Boot services backed by MongoDB
- Responsive frontend with tab navigation and conversion logs

## Quick start (Docker)

```bash
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Temperature API | http://localhost:8081 |
| Currency API | http://localhost:8082 |

Stop: `docker compose down`

> Ports `8081` and `8082` are REST APIs. Open `3000` (local) or the live demo for the UI.

## Project layout

```
Mongodb-with-API-testX/
├── tempconv/           # Temperature service (8081)
├── currencyconvertor/  # Currency service (8082)
├── frontend/           # Web UI
└── docker-compose.yml  # Full stack
```

## API endpoints

**Temperature** (`8081`)

```http
POST /api/temperatures/convert?value={value}&unit={unit}
GET  /api/temperatures/history
```

**Currency** (`8082`)

```http
POST /api/currency/convert?usdAmount={amount}
GET  /api/currency/history
```

## Tech stack

Spring Boot 4 · Java 21 · MongoDB · HTML/CSS/JS · Docker · Cloudflare Pages

## Local run (without Docker)

1. Start MongoDB on ports `27017` and `27018`
2. Run `tempconv` → port `8081`
3. Run `currencyconvertor` → port `8082`
4. Open `frontend/index.html` or serve the `frontend/` folder
