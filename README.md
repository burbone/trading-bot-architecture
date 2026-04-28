# Trading Bot Architecture (Demo)

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-green.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-ready-blue.svg)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**High-performance microservice for real-time market data collection and automated trading operations.**  
*All sensitive trading logic removed — this is a public portfolio version demonstrating architecture, multithreading, and exchange integration.*

> 🇷🇺 [Русская версия](README.ru.md)

---

## 📖 Overview

This project implements a scalable, reactive system that connects to multiple cryptocurrency exchanges, collects market data in real time, and executes trading operations based on configurable rules. The actual trading strategy is **not included** (NDA), but the codebase showcases a production-ready architecture suitable for high-frequency applications.

**Key capabilities (demonstrated):**

- Real-time price and balance fetching from exchange APIs (Bybit, KuCoin)
- Concurrent task scheduling and thread-safe state management
- RESTful API for control (start/stop) and monitoring
- Web dashboard for live portfolio tracking and trade history
- Docker containerization for easy deployment
- Automatic time synchronization with exchange servers

---

## 🏗️ Architecture

The application follows a modular layered design with strict separation of concerns:

```
src/main/java/com/mySelfCode/algo/
├── api/                        # Exchange integration (HMAC auth, REST clients)
│   ├── bybit/                  # Bybit API services (balance, orders, prices)
│   └── kucoin/                 # KuCoin API services (balance, orders, prices)
├── cfg/                        # Configuration classes (simulation mode, ports)
├── controller/                 # HTTP endpoints (starter, stop, profile, history)
├── dto/                        # Data transfer objects and concurrent registries
├── entity/                     # In-memory storage for trade history
├── service/
│   ├── market/                 # Price and balance aggregation services
│   ├── scheduler/              # Scheduled tasks for data refresh
│   ├── trading/                # Order execution and status checking
│   ├── analytics/              # Profit calculation utilities
│   └── NoOpAlgorithmStub.java  # Placeholder for trading logic (removed)
└── AlgoApplication.java
```

All exchange API calls are protected by a **simulation mode** (`bot.simulationMode=true`) that returns mock data when no real keys are present — allowing the application to run safely in a demo environment.

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker (optional)

### Run locally

```bash
git clone https://github.com/burbone/trading-bot-architecture.git
cd trading-bot-architecture
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080` with the web dashboard available immediately.

### Run with Docker

```bash
docker compose up --build
```

### Demo mode

By default, all API keys and passwords are empty and `bot.simulationMode=true` — no real exchange requests are made. You can safely explore the UI and observe scheduled task execution without any credentials.

---

## 🧪 Features (technical highlights)

### Concurrency & Resilience

- `CopyOnWriteArrayList` and `AtomicReference` for lock-free state management
- `ExecutorService` pools for parallel exchange requests
- Graceful error handling with fallback and retry logic

### Exchange Integration

- HMAC-SHA256 authentication for Bybit and KuCoin
- Real-time orderbook snapshots and balance queries
- Order placement (market) with precision handling
- Automatic server time synchronization (`BybitTimeService`) to avoid timestamp drift

### Monitoring & Control

- REST API: `POST /api/starter`, `POST /api/stop`, `GET /profile/all`, `GET /profile/history`
- Live web dashboard with real-time balance updates and trade history

### DevOps

- `Dockerfile` for optimized container builds
- `docker-compose.yml` with resource limits and log rotation
- `.gitignore` excludes all sensitive and generated files

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

## 🔒 Disclaimer

The original trading algorithm, risk management rules, and profit-generation logic are intentionally omitted from this public repository due to a Non-Disclosure Agreement (NDA). This version is provided solely to demonstrate software architecture, clean code practices, and integration capabilities.
