# ⚡ Flash Sale System

A Java Spring Boot backend demonstrating **correct concurrency handling** for high-traffic flash sales, where many users compete for limited stock at the same time.

The main goal of this project is to prevent **overselling** by handling stock updates atomically at the database level. :contentReference[oaicite:1]{index=1}

---

## 🎯 What This Project Demonstrates

- **Concurrency handling** — prevents overselling under concurrent requests
- **Database-level atomicity** — uses atomic SQL stock updates
- **Defense in depth** — SQL conditions and database constraints
- **Layered architecture** — API, service, repository, and model layers
- **Spring Boot REST API**
- **Automated concurrency testing** with JUnit 5
- **PostgreSQL database integration**

The project demonstrates why application-level locking such as `synchronized` is not sufficient for a multi-server application, while database-level atomic operations can provide shared protection. :contentReference[oaicite:2]{index=2}

---

## 🧠 The Core Problem

A naive implementation might check the stock and then decrease it:

```java
if (stock > 0) {
    stock = stock - 1;
}
```

When many users make requests simultaneously, multiple threads can read the same stock value before it is updated, causing **overselling**. :contentReference[oaicite:3]{index=3}

### ✅ Solution

The project uses an atomic SQL operation:

```sql
UPDATE flash_sales
SET remaining_stock = remaining_stock - ?
WHERE id = ? AND remaining_stock >= ?
```

If there is not enough stock, the update affects zero rows and the purchase fails. :contentReference[oaicite:4]{index=4}

---

## 🏗️ Architecture

```text
┌──────────────────────────────────────┐
│       Spring Boot REST API           │
│                                      │
│  /api/health                         │
│  /api/flash-sales                    │
│  /api/flash-sales/{id}/reserve       │
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│          Service Layer               │
│          Business Logic              │
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│        Repository Layer              │
│              JDBC                    │
│                                      │
│   Atomic Stock Reservation           │
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│          PostgreSQL                  │
│                                      │
│  users │ products │ flash_sales      │
│  orders                              │
└──────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Java 22 |
| Framework | Spring Boot 3.3.4 |
| Build Tool | Maven |
| Web Server | Embedded Tomcat |
| Database | PostgreSQL 18 |
| Database Access | JDBC |
| Connection Pool | HikariCP |
| Testing | JUnit 5 |
| Logging | SLF4J + Logback |
| JSON | Jackson |

These technologies correspond to the project's current stack. :contentReference[oaicite:5]{index=5}

---

## 📁 Project Structure

```text
flash-sale-demo/
│
├── src/
│   ├── main/
│   │   ├── java/org/example/flashsale/
│   │   │   ├── FlashSaleApplication.java
│   │   │   ├── api/
│   │   │   │   └── HealthController.java
│   │   │   ├── model/
│   │   │   │   ├── User.java
│   │   │   │   ├── Product.java
│   │   │   │   ├── FlashSale.java
│   │   │   │   ├── FlashSaleStatus.java
│   │   │   │   ├── Order.java
│   │   │   │   └── OrderStatus.java
│   │   │   ├── repository/
│   │   │   │   ├── Database.java
│   │   │   │   ├── FlashSaleRepository.java
│   │   │   │   └── ProductRepository.java
│   │   │   ├── service/
│   │   │   │   └── FlashSaleService.java
│   │   │   └── exception/
│   │   │       ├── SoldOutException.java
│   │   │       └── SaleNotActiveException.java
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│       └── java/org/example/flashsale/
│           └── repository/
│               └── FlashSaleRepositoryTest.java
│
├── docs/
│   └── screenshots/
├── pom.xml
└── README.md
```

The repository and service layers contain the core flash-sale logic, while the concurrency test verifies the stock reservation behavior. :contentReference[oaicite:6]{index=6}

---

## 🚀 Getting Started

### Prerequisites

- Java 22 JDK
- Maven
- PostgreSQL 18

### 1. Create the Database

```sql
CREATE DATABASE flashsale;
```

### 2. Configure PostgreSQL

Update:

```text
src/main/resources/application.properties
```

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/flashsale
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application runs at:

```text
http://localhost:8080
```

### 4. Test the API

```bash
curl http://localhost:8080/api/health
```

Expected response:

```json
{
  "status": "UP",
  "service": "flash-sale",
  "timestamp": 1728380293195
}
```

---

## 🧪 Concurrency Testing

The project includes automated tests that simulate many users purchasing limited stock simultaneously.

### Test Scenario 1

**100 concurrent users → 1 available item**

Expected result:

```text
1 successful purchase
99 sold-out responses
0 remaining stock
```

### Test Scenario 2

**500 concurrent users → 100 available items**

Expected result:

```text
100 successful purchases
400 sold-out responses
0 remaining stock
```

The tests use `ExecutorService` and `CountDownLatch` to simulate concurrent requests. :contentReference[oaicite:7]{index=7}

---

## 📸 Screenshots

Add screenshots to:

```text
docs/screenshots/
```

Recommended screenshots:

- `health-endpoint.png`
- `startup-log.png`
- `test-passing.png`

---

## 🗺️ Future Improvements

- JWT authentication
- User roles
- Complete flash-sale REST endpoints
- Redis stock reservations with TTL
- RabbitMQ order processing
- Docker containerization
- CI/CD pipeline
- Cloud deployment
- Load testing with Gatling

---

## 🎓 Key Learning Outcomes

This project demonstrates several important backend concepts:

- Race conditions
- Database-level concurrency control
- Atomic SQL operations
- Why `synchronized` does not scale across multiple servers
- Layered backend architecture
- Automated concurrency testing
- Defense-in-depth database design

The concurrency tests provide measurable evidence that the system can prevent overselling under concurrent load. :contentReference[oaicite:8]{index=8}

---

## 📜 License

MIT License

---

## 👤 Author

**Your Name**

Built as a backend learning project focused on **Java, Spring Boot, PostgreSQL, and concurrent system design**.