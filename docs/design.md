# FlashSale — Design Document

## 1. Product

A **fair, concurrency-safe marketplace for limited drops**.

Sellers launch flash sales with limited stock and scheduled start times. Buyers compete to reserve items when the sale goes live. The system guarantees no overselling—even under 500 simultaneous requests—using atomic database updates, database constraints, and Redis TTL reservations.

If a buyer doesn't pay within 10 minutes, the reservation expires and the stock is automatically returned to the pool.

## 2. Roles

| Role | Goal |
|---|---|
| **Buyer** | Grab limited-stock items fairly before others |
| **Seller** | Launch flash sales and manage their own products |
| **Admin** | Moderate users, products, and sales platform-wide |

A user can hold multiple roles.

---

## 3. Functional Requirements

### Authentication (all roles)

| ID | Requirement |
|---|---|
| FR-A1 | Register with email and password |
| FR-A2 | Log in and receive a JWT |
| FR-A3 | Fetch own profile |
| FR-A4 | Passwords are never stored or logged in plaintext |
| FR-A5 | Users can hold roles (BUYER, SELLER, ADMIN) |
| FR-A6 | Log out by discarding the token on the client |

### Buyer

| ID | Requirement |
|---|---|
| FR-B1 | Browse upcoming, active, and ended flash sales |
| FR-B2 | View sale details (product, price, stock, countdown) |
| FR-B3 | Reserve quantity, subject to stock and per-user limits |
| FR-B4 | Hold reservations for 10 minutes using TTL |
| FR-B5 | Confirm reservation → CONFIRMED |
| FR-B6 | Automatically expire unconfirmed reservations → EXPIRED and release stock |
| FR-B7 | View personal order history |
| FR-B8 | Cancel reservations manually |
| FR-B9 | Prevent exceeding per-user quantity limits |
| FR-B10 | Prevent reservations before the sale starts or after it ends |
| FR-B11 | Initiate payment for a RESERVED order via Stripe |
| FR-B12 | Create a Stripe PaymentIntent for the order amount |
| FR-B13 | Move the order from RESERVED → CONFIRMED after successful payment |
| FR-B14 | Keep the order RESERVED after failed payment, allowing retries until TTL expiry |
| FR-B15 | Redirect the buyer to a Stripe-hosted payment page |
| FR-B16 | Mark the order CONFIRMED after webhook confirmation |
| FR-B17 | Make payment idempotent so retries don't double-charge |
| FR-B18 | Refunds are out of scope for v1 |
| FR-B19 | Send a reminder email when reservation TTL is under 2 minutes |
| FR-B20 | Do not send a reminder if the buyer confirms payment |

### Seller

| ID | Requirement |
|---|---|
| FR-S1 | Register as a seller |
| FR-S2 | Create products (name, description, image, price) |
| FR-S3 | Edit or delete own products |
| FR-S4 | List own products |
| FR-S5 | Create flash sales for own products |
| FR-S6 | Edit or cancel own sales before they start |
| FR-S7 | View own sales dashboard |
| FR-S8 | See remaining stock in near real time |
| FR-S9 | Prevent creating a sale where the sale price exceeds the regular price |

### Admin

| ID | Requirement |
|---|---|
| FR-AD1 | View all users with pagination |
| FR-AD2 | Change user roles |
| FR-AD3 | Suspend or delete users |
| FR-AD4 | View all products |
| FR-AD5 | Remove any product |
| FR-AD6 | View all flash sales |
| FR-AD7 | Cancel any active flash sale |
| FR-AD8 | View platform-wide statistics |

### Cross-cutting

| ID | Requirement |
|---|---|
| FR-SY1 | Health check reports database and Redis status |
| FR-SY2 | Return consistent error JSON: `{error, message, timestamp}` |
| FR-SY3 | Require JWT for protected endpoints |
| FR-SY4 | Enforce role restrictions on protected endpoints |
| FR-SY5 | Automatically expire reservations using a background sweeper |
| FR-SY6 | Ensure concurrent reservation requests never oversell |

---

## 4. Non-Functional Requirements

### Performance

| ID | Requirement |
|---|---|
| NFR-P1 | Reservation response time under 200 ms at p95 |
| NFR-P2 | Sales listing response time under 100 ms at p95 |
| NFR-P3 | Handle 500 concurrent reservations for a single sale |
| NFR-P4 | Payment initiation under 500 ms at p95, excluding Stripe redirect |
| NFR-P5 | Webhook processing under 200 ms at p95 |
| NFR-P6 | Send emails asynchronously so request threads never wait |

### Scalability

| ID | Requirement |
|---|---|
| NFR-SC1 | Reservation logic works across multiple backend instances |
| NFR-SC2 | Reuse pooled database connections through HikariCP |
| NFR-SC3 | Use Redis for temporary state and PostgreSQL for durable state |

### Reliability & Consistency

| ID | Requirement |
|---|---|
| NFR-R1 | **Never oversell stock** |
| NFR-R2 | Return stock from expired reservations to the available pool |
| NFR-R3 | Degrade gracefully if Redis is unavailable |
| NFR-R4 | Prevent failed inserts from corrupting stock counts |
| NFR-R5 | Make payment processing idempotent so retries don't double-charge |
| NFR-R6 | Keep order status updates transactionally consistent |
| NFR-R7 | Failed payments do not release reservations |
| NFR-R8 | Process Stripe webhooks at least once, with idempotent handlers |
| NFR-R9 | Webhooks for expired reservations must not resurrect them |
| NFR-R10 | Email failures never block the reservation flow |

### Security

| ID | Requirement |
|---|---|
| NFR-SE1 | Hash passwords with BCrypt using at least 10 rounds |
| NFR-SE2 | Use HMAC-SHA256 JWT with a secret of at least 32 bytes |
| NFR-SE3 | Load secrets from environment variables; never commit them |
| NFR-SE4 | Use parameterized SQL only |
| NFR-SE5 | Enforce role-based access control on all protected endpoints |
| NFR-SE6 | Use generic login errors to prevent user enumeration |
| NFR-SE7 | The server never sees or stores raw card numbers; Stripe handles PCI requirements |
| NFR-SE8 | Verify Stripe webhook signatures |
| NFR-SE9 | Load the Stripe secret key from an environment variable |
| NFR-SE10 | Use HTTPS for Stripe API calls |
| NFR-SE11 | Calculate payment amounts server-side; clients cannot specify prices |
| NFR-SE12 | Load the email provider API key from an environment variable |

### Maintainability

| ID | Requirement |
|---|---|
| NFR-M1 | Use a layered architecture |
| NFR-M2 | Keep business logic out of controllers and repositories |
| NFR-M3 | Use DTOs to separate API contracts from domain models |
| NFR-M4 | Implement a global exception handler |
| NFR-M5 | Use constructor injection for testability |

### Testability

| ID | Requirement |
|---|---|
| NFR-T1 | Unit-test every service method |
| NFR-T2 | Use multithreaded tests to verify concurrency guarantees |
| NFR-T3 | Run integration tests against real PostgreSQL and Redis instances |
| NFR-T4 | Run the full test suite in CI on every push |

### Observability

| ID | Requirement |
|---|---|
| NFR-O1 | Health endpoint reports database and Redis status |
| NFR-O2 | Log reservation, confirmation, and expiration events |
| NFR-O3 | Add distributed tracing in a later phase |
| NFR-O4 | Log payment events with order and Stripe PaymentIntent IDs |
| NFR-O5 | Make failed payment attempts visible in logs |
| NFR-O6 | Log email sends with order and recipient details |

### Deployability

| ID | Requirement |
|---|---|
| NFR-D1 | Support Docker and Docker Compose |
| NFR-D2 | Configure the application through environment variables |
| NFR-D3 | Deploy to a publicly accessible URL |
| NFR-D4 | Use environment-specific Stripe keys for test and live modes |
| NFR-D5 | Make the webhook endpoint publicly reachable |

---

## 5. Architecture

**Client–Server architecture with a Layered Architecture inside the server.**

```text
┌─────────────────────────────────────────────┐
│  CLIENT — React SPA (browser)               │
└──────────────────────┬──────────────────────┘
                       │ HTTPS + JSON + JWT
                       ▼
┌─────────────────────────────────────────────┐
│  SERVER — Spring Boot (monolith)            │
│                                             │
│  Presentation Layer   Controllers + DTOs    │
│           ▼                                 │
│  Business Layer       Services              │
│           ▼                                 │
│  Persistence Layer    Repositories          │
│                                             │
│  Cross-cutting: Security, Config, Exceptions│
└──────────┬─────────────────────────┬────────┘
           ▼                         ▼
    ┌──────────────┐          ┌────────────┐
    │ PostgreSQL   │          │ Redis      │
    │ (durable)    │          │ (temporary)│
    └──────────────┘          └────────────┘
```

**Client–Server** describes the overall system: a browser communicates with a backend over HTTP.

**Layered Architecture** describes how the backend code is organized internally.

Both patterns work together in this application.

### Why this architecture?

- **Simple:** Suitable for a team of 1–3 developers.
- **Testable:** Each layer can be tested independently.
- **Correct:** Business operations can be coordinated through transaction boundaries.
- **Evolvable:** Layers can become separate service boundaries later if needed.
- **Standard:** Follows common Spring Boot application design practices.

---

## 6. Concurrency Model

The system uses three layers of defense to prevent overselling.

### 1. Atomic SQL Update

```sql
UPDATE flash_sales
SET remaining_stock = remaining_stock - ?
WHERE id = ? AND remaining_stock >= ?;
```

The database coordinates concurrent updates. If insufficient stock remains, no rows are updated, and the reservation fails.

### 2. Database CHECK Constraint

```sql
CHECK (remaining_stock >= 0)
```

Even if application logic fails to prevent a negative stock value, the database rejects the invalid state.

### 3. Redis TTL Reservation

- Reservations are held for 10 minutes.
- Unconfirmed reservations expire automatically.
- A background sweeper releases stock from expired reservations.

Together, these mechanisms are designed to prevent overselling and recover stock from expired reservations. Redis failures must be handled without compromising the database's stock constraints.

---

## 7. Scope

### v1 — This Release

- Authentication: registration, login, JWT, and roles
- Buyer flow: browse sales, reserve items, confirm orders, and view order history
- Seller flow: create products and schedule sales
- Admin flow: basic platform moderation
- Redis TTL reservations and background sweeper
- Stripe payments in test mode with webhooks
- One email reminder before reservation expiry (T−2 minutes)
- React frontend
- Deployment to public URLs

### v2 — Future Improvements

- Discount codes
- Seller analytics dashboard
- Product reviews
- Multi-currency support
- Real-time stock updates using WebSockets

### Explicitly Out of Scope for v1

- Wishlists
- Reviews
- Chat
- Social login
- Two-factor authentication (2FA)
- Shipping
- Refunds

---

## 8. Key Decisions

| Decision | Choice | Why |
|---|---|---|
| Architecture | Client–Server + Layered | Simple, testable, and evolvable |
| Authentication | JWT + BCrypt | Standard approach for stateless APIs |
| Concurrency | Atomic SQL + CHECK constraint + Redis TTL | Layered defense against overselling and expired reservations |
| Payments | Stripe test mode | Enables payment integration without processing real payments |
| Email | One reminder only | Supports the core user flow while keeping scope manageable |
| Frontend | React + Vite + Tailwind CSS | Widely used technologies that support rapid development |
| Deployment | Docker + managed platforms | Supports portable deployment and public access |
