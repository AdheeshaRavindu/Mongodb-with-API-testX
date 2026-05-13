# 🔄 ConvertHub — Temperature & Currency Converter

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-4EA94B?style=for-the-badge&logo=mongodb&logoColor=white)
![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)

**A full-stack microservices application featuring Temperature and Currency conversion with a calm, organic UI theme, built with Spring Boot and MongoDB.**

[Features](#-features) • [Live Frontend](#-live-frontend) • [UI Theme](#-ui-theme) • [Architecture](#-architecture) • [Getting Started](#-getting-started) • [API Reference](#-api-reference)

</div>

---

## ✨ Features

- 🌡️ **Temperature Conversion** — Convert between Celsius, Fahrenheit, and Kelvin with smart unit normalization
- 💱 **Currency Conversion** — Convert USD to LKR with a configurable exchange rate
- 📊 **Conversion History** — All conversions are logged and persisted in MongoDB with timestamps
- 🎨 **Modern UI** — Minimal, organic dashboard with linen-style background and warm neutral tones
- 🏗️ **Microservices Architecture** — Each converter runs as an independent Spring Boot service
- 🔄 **RESTful APIs** — Clean REST endpoints with proper error handling and CORS support

---

## 🌐 Live Frontend

- Production URL: **https://converthub-mongodb-api.pages.dev**
- Hosting: **Cloudflare Pages**
- Frontend folder deployed: `frontend/`

---

## 🎨 UI Theme

The frontend now follows an earthy, professional palette with clear semantic contrast:

- **Background (linen beige):** `#EDE6DB`
- **Headline primary:** `#1F4D3C`
- **Body text / muted copy:** `#3C3C3D` / `#6C6C6C`
- **Header + footer surface:** `#4D4940`
- **Currency action:** `#0D6C68`
- **Temperature action (neutral):** `#8C8983`
- **Accent dots:** `#C36B42`, `#C09B61`, `#1F4D3C`
- **Footer API chip:** `#1D5D51`

Design goals:
- clear visual hierarchy
- reduced visual noise
- warm, organic feel with professional contrast

---

## 🏗 Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    ConvertHub Frontend                    │
│              (HTML / CSS / JavaScript)                    │
│                  Opens on any browser                     │
└──────────┬──────────────────────────────┬────────────────┘
           │                              │
     Port 8081                      Port 8082
           │                              │
┌──────────▼──────────┐     ┌─────────────▼───────────────┐
│  Temperature Conv.  │     │    Currency Converter        │
│  (Spring Boot 4.0)  │     │    (Spring Boot 4.0)        │
│                     │     │                             │
│  • Controller       │     │  • Controller               │
│  • Service          │     │  • Service                  │
│  • Repository       │     │  • Repository               │
│  • Model            │     │  • Model                    │
└──────────┬──────────┘     └─────────────┬───────────────┘
           │                              │
     Port 27017                     Port 27018
           │                              │
┌──────────▼──────────┐     ┌─────────────▼───────────────┐
│   MongoDB (temp_db) │     │   MongoDB (currency_db)     │
└─────────────────────┘     └─────────────────────────────┘
```

---

## 📁 Project Structure

```
Mongodb-with-API-testX/
│
├── tempconv/                          # 🌡️ Temperature Converter Microservice
│   ├── pom.xml                        # Maven dependencies (Spring Boot 4.0.5)
│   ├── mvnw / mvnw.cmd               # Maven wrapper scripts
│   └── src/
│       ├── main/
│       │   ├── java/com/nima/tempconv/
│       │   │   ├── TempconvApplication.java        # Entry point + CORS config
│       │   │   ├── controller/
│       │   │   │   └── TemperatureController.java   # REST endpoints
│       │   │   ├── service/
│       │   │   │   └── TemperatureService.java      # Conversion logic
│       │   │   ├── model/
│       │   │   │   └── TemperatureLog.java           # MongoDB document
│       │   │   └── repository/
│       │   │       └── TemperatureRepository.java    # Data access layer
│       │   └── resources/
│       │       └── application.yaml                  # Server & DB config
│       └── test/
│           └── java/.../TempconvApplicationTests.java
│
├── currencyconvertor/                 # 💱 Currency Converter Microservice
│   ├── pom.xml                        # Maven dependencies (Spring Boot 4.0.6)
│   ├── mvnw / mvnw.cmd               # Maven wrapper scripts
│   └── src/
│       ├── main/
│       │   ├── java/com/usdtolkr/currencyconvertor/
│       │   │   ├── CurrencyconvertorApplication.java  # Entry point + CORS
│       │   │   ├── controller/
│       │   │   │   └── CurrencyController.java         # REST endpoints
│       │   │   ├── service/
│       │   │   │   └── CurrencyService.java             # Exchange logic
│       │   │   ├── model/
│       │   │   │   └── CurrencyLog.java                 # MongoDB document
│       │   │   └── Repository/
│       │   │       └── CurrencyRepository.java          # Data access layer
│       │   └── resources/
│       │       └── application.yaml                     # Server & DB config
│       └── test/
│           └── java/.../CurrencyconvertorApplicationTests.java
│
└── frontend/                          # 🎨 Web Frontend
    ├── index.html                     # Main HTML structure
    ├── style.css                      # Organic UI theme + responsive styling
    └── app.js                         # API integration & DOM logic
```

---

## 🚀 Getting Started

### Prerequisites

| Requirement | Version |
|-------------|---------|
| **Java JDK** | 21+ |
| **Maven** | 3.9+ (or use included `mvnw`) |
| **MongoDB** | 6.0+ |
| **Browser** | Any modern browser |

### 1️⃣ Start MongoDB Instances

The application requires **two separate MongoDB instances**:

```bash
# Terminal 1: MongoDB for Temperature Converter (port 27017)
mongod --port 27017 --dbpath /data/temp_db

# Terminal 2: MongoDB for Currency Converter (port 27018)
mongod --port 27018 --dbpath /data/currency_db
```

> **💡 Tip:** If using Docker, you can spin up both with:
> ```bash
> docker run -d -p 27017:27017 --name mongo-temp mongo:latest
> docker run -d -p 27018:27017 --name mongo-currency mongo:latest
> ```

### 2️⃣ Start Temperature Converter Backend

```bash
cd tempconv
./mvnw spring-boot:run
```

✅ Server starts on **http://localhost:8081**

### 3️⃣ Start Currency Converter Backend

```bash
cd currencyconvertor
./mvnw spring-boot:run
```

✅ Server starts on **http://localhost:8082**

### 4️⃣ Open the Frontend

Simply open `frontend/index.html` in your browser — no build step required!

```bash
# Or use a live server
cd frontend
npx serve .
```

---

## 📡 API Reference

### 🌡️ Temperature Converter — `http://localhost:8081`

#### Convert Temperature

```http
POST /api/temperatures/convert?value={value}&unit={unit}
```

| Parameter | Type     | Description                                      |
|-----------|----------|--------------------------------------------------|
| `value`   | `double` | **Required.** Temperature value to convert        |
| `unit`    | `string` | **Required.** Input unit (`celsius`, `fahrenheit`, `kelvin`, `c`, `f`, `k`) |

**Example Request:**

```bash
curl -X POST "http://localhost:8081/api/temperatures/convert?value=100&unit=celsius"
```

**Example Response:**

```json
{
  "id": "664a1b2c3d4e5f6a7b8c9d0e",
  "inputTemperature": 100.0,
  "inputUnit": "Celsius",
  "outputTemperature": 212.0,
  "outputUnit": "Fahrenheit",
  "timestamp": "2026-05-08T00:15:30.123Z"
}
```

#### Get Conversion History

```http
GET /api/temperatures/history
```

**Example:**

```bash
curl http://localhost:8081/api/temperatures/history
```

---

### 💱 Currency Converter — `http://localhost:8082`

#### Convert USD to LKR

```http
POST /api/currency/convert?usdAmount={amount}
```

| Parameter   | Type     | Description                           |
|-------------|----------|---------------------------------------|
| `usdAmount` | `double` | **Required.** USD amount to convert   |

**Example Request:**

```bash
curl -X POST "http://localhost:8082/api/currency/convert?usdAmount=50"
```

**Example Response:**

```json
{
  "id": "664a1b2c3d4e5f6a7b8c9d0f",
  "inputAmount": 50.0,
  "inputCurrency": "USD",
  "outputAmount": 15000.0,
  "outputCurrency": "LKR",
  "exchangeRate": 300.0,
  "timestamp": "2026-05-08T00:20:45.456"
}
```

#### Get Conversion History

```http
GET /api/currency/history
```

**Example:**

```bash
curl http://localhost:8082/api/currency/history
```

---

## 🛠 Tech Stack

| Layer        | Technology                  | Purpose                              |
|--------------|-----------------------------|--------------------------------------|
| **Backend**  | Spring Boot 4.0 (Java 21)  | REST API microservices               |
| **Database** | MongoDB                     | NoSQL document storage               |
| **ORM**      | Spring Data MongoDB         | Repository pattern data access       |
| **Utility**  | Lombok                      | Boilerplate code reduction           |
| **Frontend** | HTML5 / CSS3 / JavaScript   | Responsive web interface             |
| **Fonts**    | Google Fonts (Outfit, IBM Plex Mono) | Modern typography            |
| **Build**    | Maven                       | Dependency management & build tool   |

---

## 🧪 Running Tests

```bash
# Temperature Converter tests
cd tempconv
./mvnw test

# Currency Converter tests
cd currencyconvertor
./mvnw test
```

---

## ⚙️ Configuration

### Temperature Converter (`tempconv/src/main/resources/application.yaml`)

```yaml
spring:
  application:
    name: tempconv
  data:
    mongodb:
      uri: mongodb://localhost:27017/temp_db

server:
  port: 8081
```

### Currency Converter (`currencyconvertor/src/main/resources/application.yaml`)

```yaml
spring:
  application:
    name: currencyconvertor
  mongodb:
    uri: mongodb://localhost:27018/currency_db

server:
  port: 8082
```

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feat/amazing-feature`)
3. **Commit** your changes (`git commit -m 'feat: add amazing feature'`)
4. **Push** to the branch (`git push origin feat/amazing-feature`)
5. **Open** a Pull Request

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

<div align="center">

**Built with ❤️ using Spring Boot & MongoDB**

*System Oriented Computing — Lab Practical 02*

</div>
