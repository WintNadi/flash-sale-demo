# FlashSale

A **fair, concurrency-safe marketplace for limited drops**.

Sellers launch flash sales with limited stock. Buyers compete to reserve items when the sale goes live. The system prevents overselling—even under 500 simultaneous requests—using atomic database updates, database constraints, and Redis TTL reservations. If a buyer doesn't pay within 10 minutes, the stock is automatically released.

> **Live demo:** Coming soon  
> **API base URL:** Coming soon

---

## Documentation

- [Design Document](docs/design.md) — Product definition, functional and non-functional requirements, and architecture decisions
- [Database Schema](docs/schema.sql) — Database tables and constraints
- [API Tests](src/test/auth.http) — HTTP test scripts

---

## Why This Project Is Interesting

Every flash sale application faces the same challenge:

> *When 100 users click “Buy” at the exact same second, only the available number of buyers should succeed.*

Naive implementations can oversell. This project addresses the problem using three layers of protection:

1. **Atomic SQL update** — Decreases stock only when sufficient stock remains.
2. **Database CHECK constraint** — Prevents stock from becoming negative.
3. **Redis TTL reservations** — Automatically expires unconfirmed reservations.

Concurrency tests verify the stock limits under simultaneous requests.

---

## Features

### Buyer
- Register and log in using JWT authentication
- Browse upcoming, active, and ended flash sales
- Reserve stock based on availability and per-user limits
- Get a 10-minute reservation window
- Complete payments through Stripe in test mode
- View personal order history
- Cancel reservations and release stock

### Seller
- Register as a seller
- Create and manage products
- Schedule flash sales with limited stock
- View a sales dashboard

### Admin
- Moderate users, products, and flash sales
- View platform-wide statistics

### System
- Atomic stock reservation to prevent overselling
- Redis TTL reservations with a background sweeper
- Email reminder two minutes before reservation expiry
- Health check for database and Redis status

---

## Architecture

```text
┌───────────────────────────────────────────────┐
│  Client — React SPA (browser)                 │
└───────────────────────┬───────────────────────┘
                        │ HTTPS + JSON + JWT
                        ▼
┌───────────────────────────────────────────────┐
│  Spring Boot Monolith                         │
│                                               │
│  Presentation Layer   — Controllers + DTOs    │
│  Business Layer       — Services              │
│  Persistence Layer    — JdbcTemplate Repos    │
│  Cross-cutting        — Security, Config      │
└────────┬──────────────────────────────┬───────┘
         ▼                              ▼
   ┌──────────────┐              ┌──────────────┐
   │ PostgreSQL   │              │ Redis        │
   │ Durable data │              │ Temporary    │
   └──────────────┘              └──────────────┘
```

**Architecture style:** Client–Server with Layered Architecture.

This design keeps the application simple and testable while allowing it to evolve as the project grows. See the [design document](docs/design.md) for more details.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 22 |
| Build Tool | Maven |
| Framework | Spring Boot 3.3.4 |
| Security | Spring Security 6, JJWT 0.12 |
| Database | PostgreSQL 18 |
| Cache and TTL | Redis 7 |
| Database Access | JdbcTemplate, HikariCP |
| Payments | Stripe (test mode) |
| Email | SendGrid / Mailtrap |
| Frontend | React, Vite, Tailwind CSS |
| Testing | JUnit 5, Spring Test |
| Deployment | Docker, Render, Vercel |
| CI/CD | GitHub Actions |

---

## API Endpoints

| Method | Path | Authentication | Description |
|---|---|---|---|
| GET | `/api/health` | Public | Check database and Redis status |
| POST | `/api/auth/register` | Public | Register an account |
| POST | `/api/auth/login` | Public | Log in and receive a JWT |
| GET | `/api/auth/me` | JWT | Get the current user |
| GET | `/api/flash-sales` | Public | List flash sales |
| GET | `/api/flash-sales/{id}` | Public | View sale details |
| GET | `/api/flash-sales/reservations/{orderId}` | Public | Inspect a reservation |
| POST | `/api/flash-sales/{id}/reserve` | JWT | Reserve stock |
| POST | `/api/orders/{id}/confirm` | JWT | Confirm an order |
| POST | `/api/orders/{id}/cancel` | JWT | Cancel a reservation |
| GET | `/api/orders/me` | JWT | View personal order history |

### Example: Reserve Stock

**Request**

```http
POST /api/flash-sales/10/reserve
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "quantity": 1
}
```

**Response — `201 Created`**

```json
{
  "orderId": 1,
  "userId": 1,
  "flashSaleId": 10,
  "quantity": 1,
  "totalPrice": 499.0,
  "remainingStock": 99,
  "createdAt": "2026-10-10T15:05:00"
}
```

The `userId` is derived from the JWT, preventing clients from impersonating another user by submitting a different ID.

---

## Concurrency Proof

The project tests concurrency behavior using database operations rather than relying only on mocks.

### Repository-Level Tests

`src/test/java/org/example/flashsale/repository/FlashSaleRepositoryTest.java`

- **100 concurrent threads, stock = 1:** Exactly 1 successful reservation and 99 rejected attempts.
- **500 concurrent threads, stock = 100:** Exactly 100 successful reservations and 400 rejected attempts.

In both scenarios, stock must never become negative.

### HTTP-Level Tests

`src/test/java/org/example/flashsale/api/FlashSaleConcurrencyHttpTest.java`

- Sends real HTTP POST requests to a running Spring Boot server.
- Verifies stock limits under concurrent requests.

### Why It Matters

Java's `synchronized` keyword only coordinates threads within a single JVM. Atomic SQL updates coordinate stock changes through the shared database, even when requests reach different backend instances.

---

## Getting Started

### Prerequisites

- Java 22 (JDK)
- Maven
- PostgreSQL 18 running on `localhost:5432`
- Redis 7, or Memurai on Windows, running on `localhost:6379`

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/flash-sale-demo.git
cd flash-sale-demo
```

Replace `YOUR_USERNAME` with your GitHub username.

### 2. Create the Database

Connect to PostgreSQL and run:

```sql
CREATE DATABASE flashsale;
```

Then execute the [database schema](docs/schema.sql).

### 3. Configure Environment Variables

Copy the environment template:

```bash
cp .env.example .env
```

Configure the required values in your environment:

```env
DB_USERNAME=postgres
DB_PASSWORD=your-postgres-password
JWT_SECRET=your-generated-secret
STRIPE_SECRET_KEY=your-stripe-test-secret-key
STRIPE_WEBHOOK_SECRET=your-stripe-webhook-secret
SENDGRID_API_KEY=your-sendgrid-api-key
```

Use your actual local configuration and test credentials. Never commit `.env` or real secrets to GitHub.

### 4. Run the Application

Start the application from IntelliJ IDEA or run:

```bash
mvn spring-boot:run
```

Make sure PostgreSQL and Redis are running and that the required environment variables are available to the application.

### 5. Verify the Application

```bash
curl http://localhost:8080/api/health
```

Example response:

```json
{
  "status": "UP",
  "service": "flash-sale",
  "timestamp": 1728800000000,
  "redis": "UP"
}
```

The response values depend on the application's current state and implementation.

---

## Authentication

Protected endpoints require a JWT.

```http
Authorization: Bearer <token>
```

### Authentication Flow

1. `POST /api/auth/register` — Create an account.
2. `POST /api/auth/login` — Receive a JWT.
3. Include the token in requests to protected endpoints.

### Security Measures

- Passwords are hashed using BCrypt.
- JWTs are signed using HMAC-SHA256.
- User identity is derived from the token, not trusted from the request body.
- Login errors are generic to help prevent user enumeration.

---

## Redis TTL Reservations

### The Problem

When a buyer reserves stock but never completes payment, the reserved stock must not remain unavailable forever.

### The Solution

1. The buyer reserves stock. PostgreSQL updates the stock count and records the order.
2. Redis stores a reservation key, `reservation:{orderId}`, with a 10-minute TTL.
3. A background sweeper checks for expired reservations every 30 seconds.
4. Expired reservations are marked `EXPIRED`, and their stock is returned to the available pool.

### Debug Endpoint

```http
GET /api/flash-sales/reservations/{orderId}
```

Example response:

```json
{
  "orderId": 5,
  "ttlSeconds": 542,
  "active": true,
  "reservation": {
    "orderId": 5,
    "userId": 1,
    "flashSaleId": 10,
    "quantity": 1
  }
}
```

---

## Roadmap

### Backend Core

- [x] Phase 1 — Pure Java domain and concurrency tests
- [x] Phase 2 — PostgreSQL integration and atomic stock reservation
- [x] Phase 3 — Spring Boot REST API and JdbcTemplate
- [x] Phase 4 — JWT authentication and protected endpoints
- [x] Phase 5.1 — Redis integration and health check
- [x] Phase 5.2 — TTL reservations stored in Redis
- [ ] Phase 5.3 — Background sweeper for expired reservations
- [ ] Phase 5.4 — Order confirmation
- [ ] Phase 5.5 — Order history and integration tests

### Infrastructure

- [ ] Phase 6 — Docker and Docker Compose
- [ ] Phase 7 — CORS configuration and API improvements

### Frontend

- [ ] Phase 8 — React, Vite, and Tailwind CSS MVP
- [ ] Phase 9 — UI improvements, animations, and responsive design

### Real Commerce

- [ ] Phase 10 — Stripe integration in test mode with webhooks
- [ ] Phase 11 — Email reminders before reservation expiry

### Deployment and Quality

- [ ] Phase 12 — Deploy backend and frontend
- [ ] Phase 13 — CI/CD with GitHub Actions
- [ ] Phase 14 — Load testing with Gatling

### v2 — Future Improvements

- [ ] Discount codes
- [ ] Seller analytics dashboard
- [ ] Product reviews
- [ ] Real-time stock updates using WebSockets

---

## Project Structure

```text
flash-sale-demo/
├── docs/
│   ├── design.md
│   ├── schema.sql
│   └── screenshots/
├── src/
│   ├── main/
│   │   ├── java/org/example/flashsale/
│   │   │   ├── FlashSaleApplication.java
│   │   │   ├── api/
│   │   │   ├── config/
│   │   │   ├── exception/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/org/example/flashsale/
│           ├── api/
│           ├── config/
│           └── repository/
├── .env.example
├── .gitignore
├── pom.xml
├── README.md
└── LICENSE
```

---

## Contributing

Issues and pull requests are welcome. For major changes, please open an issue first to discuss the proposed changes.

---

