# Food Delivery Order Management

A Spring Boot REST API for managing a food-delivery workflow from catalog setup through order delivery and review.

The project implements the supplied [problem statement](Food%20Delivery%20Order%20Management.pdf) as one Java application backed by one relational database. It has no frontend and no production authentication layer.

## Contents

- [Features](#features)
- [Technology](#technology)
- [Getting started](#getting-started)
- [Swagger and API documentation](#swagger-and-api-documentation)
- [Identity and roles](#identity-and-roles)
- [Architecture](#architecture)
- [Data model](#data-model)
- [Order lifecycle](#order-lifecycle)
- [API reference](#api-reference)
- [Search and opening hours](#search-and-opening-hours)
- [Example order flow](#example-order-flow)
- [Business rules](#business-rules)
- [Errors and validation](#errors-and-validation)
- [Configuration](#configuration)
- [Project structure](#project-structure)
- [Scope and limitations](#scope-and-limitations)

## Features

### User and role management

- Public customer registration with username, full name, email address, and phone number.
- Admin creation of `ADMIN`, `OWNER`, `CUSTOMER`, and `PARTNER` users.
- Unique username, email, and phone-number validation.
- Profile lookup through `X-User-Id`.
- Role-based and ownership-based authorization for every protected operation.

### City and restaurant management

- Admin creation and updating of serviceable cities.
- Admin creation and updating of restaurants, including their owner, city, address, and active state.
- Public paginated restaurant listing with city filtering.
- Case-insensitive restaurant-name search.
- Individual restaurant details with current opening status.
- Restaurant activation checks that also respect the parent city's active state.

### Opening hours

- Owner or admin configuration of daily restaurant opening and closing times.
- Support for normal daytime and overnight schedules, such as `18:00` to `02:00`.
- Configurable restaurant timezone, defaulting to `Asia/Kolkata`.
- `openNow` information in restaurant responses.
- Automatic rejection of new orders while a restaurant is inactive or closed.
- Optional all-day operation when opening hours are not configured.

### Menu and inventory management

- Public paginated menu browsing for each restaurant.
- Case-insensitive search across menu-item names and descriptions.
- Owner or admin creation and updating of menu items.
- Management of item name, description, price, stock, and availability.
- Signed stock adjustments for restocking or correcting inventory.
- Transactional stock reservation during checkout.
- Automatic stock restoration after order cancellation or rejection.
- Database row locking and deterministic lock ordering to prevent overselling.

### Ordering and payments

- Customer ordering from one restaurant with multiple menu items.
- Server-side price calculation using the current menu prices.
- Historical order lines that preserve item names and prices after menu changes.
- Required customer-scoped `Idempotency-Key` support to prevent duplicate orders.
- Safe replay of the same order request and rejection of conflicting key reuse.
- Simulated successful and declined payments using `TEST_SUCCESS` and `TEST_DECLINE`.
- Atomic checkout: failed payment, invalid stock, or validation errors roll back the entire order.
- Payment states and simulated refunds for rejected or cancelled orders.
- Paginated order listing with role-specific visibility and optional status filtering.
- Detailed order lookup including items, payment, assigned partner, and event history.

### Order lifecycle

- Successful lifecycle: `PLACED → ACCEPTED → PREPARING → OUT_FOR_DELIVERY → DELIVERED`.
- Customer cancellation while an order is eligible for cancellation.
- Restaurant-owner rejection while an order is eligible for rejection.
- Actor checks so only the correct customer, restaurant owner, or assigned partner can perform each transition.
- Persistent order-event history for auditing each lifecycle change.

### Delivery-partner management

- Admin creation, listing, activation, deactivation, and city assignment of delivery-partner profiles.
- Partner profile lookup with current active-order information.
- Discovery of claimable orders in the partner's city.
- Atomic order claiming so simultaneous partners cannot claim the same order.
- One active order per delivery partner.
- Prevention of city changes or deactivation while a partner has an active order.
- Automatic partner release when an order is delivered or otherwise completed.

### Reviews and ratings

- One review per delivered order.
- Reviews restricted to the customer who placed the order.
- Ratings from 1 to 5 with an optional comment.
- Public paginated restaurant reviews and calculated average rating.

### Notifications and background processing

- Transactional `OrderEvent` creation whenever an order changes.
- Scheduled processing of pending events.
- Asynchronous notification dispatch through a configurable thread pool.
- In-application notification inboxes for customers, restaurant owners, and assigned partners.
- Reliable event acknowledgement together with notification persistence.

### API quality and reliability

- Swagger UI and generated OpenAPI JSON documentation.
- Jakarta Bean Validation for request bodies, headers, query parameters, and pagination.
- Consistent JSON error responses for validation, authorization, missing resources, conflicts, and payment declines.
- Rejection of unknown JSON fields and unsupported endpoints or methods.
- Native SQL queries, pagination counts, database constraints, and pessimistic locking for critical workflows.
- Integration coverage for every controller route, complete lifecycles, rollback behavior, asynchronous notifications, and concurrency conflicts.

## Technology

| Component | Technology |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.5.16 |
| Web | Spring MVC |
| Persistence | Spring Data JPA and Hibernate |
| Database | H2 file database |
| Validation | Jakarta Bean Validation |
| API documentation | Springdoc OpenAPI 2.8.17 and Swagger UI |
| Build | Maven |
| Boilerplate reduction | Lombok for JPA entities |

## Getting started

### Prerequisites

- JDK 17
- Maven 3.6.3 or newer

Confirm that Maven is using Java 17:

```bash
mvn -version
```

### Run with Maven

```bash
mvn spring-boot:run
```

### Build and run the JAR

```bash
mvn -DskipTests clean package
java -jar target/food-delivery-1.0.0.jar
```

The application starts at `http://localhost:8080`.

The default database is stored at `./data/food-delivery.mv.db`. Hibernate creates or updates the schema automatically.

### Use a temporary in-memory database

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:mem:food-delivery"
```

## Swagger and API documentation

Start the application, then open:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Swagger UI lists the request schemas, response schemas, parameters, and available operations generated from the Spring controllers and DTOs.

## Identity and roles

This project intentionally does not use Spring Security, passwords, sessions, or tokens.

Protected operations receive an `X-User-Id` header. The application loads that user from the database and applies role and ownership checks.

New user profiles require `username`, `fullName`, `email`, and `phoneNumber`. Email addresses are normalized to lowercase, phone numbers use an E.164-style format such as `+919876543210`, and username, email, and phone number must each be unique.

```http
X-User-Id: 4
```

This header is a demonstration identity selector, not secure authentication. A production system must replace it with authenticated user identity.

### Roles

| Role | Responsibilities |
| --- | --- |
| `ADMIN` | Manage users, cities, restaurants, and delivery-partner profiles; read all orders |
| `OWNER` | Manage owned restaurant menus and opening hours; progress owned restaurant orders |
| `CUSTOMER` | Place, read, cancel, and review personal orders |
| `PARTNER` | View local available orders, claim one order, and complete delivery |

### Seed data

When `DEMO_DATA=true` and the users table is empty, the application creates:

| ID | Username | Role | Associated data |
| --- | --- | --- | --- |
| 1 | `admin` | `ADMIN` | Administrative account |
| 2 | `owner` | `OWNER` | Spice Kitchen in Mumbai |
| 3 | `owner2` | `OWNER` | Garden Cafe in Pune |
| 4 | `customer` | `CUSTOMER` | Example customer |
| 5 | `customer2` | `CUSTOMER` | Second example customer |
| 6 | `partner` | `PARTNER` | Mumbai delivery-partner profile |
| 7 | `partner2` | `PARTNER` | Second Mumbai delivery-partner profile |

The seed also creates Mumbai and Pune, two restaurants, and three menu items. IDs are predictable only for a new database; API responses are authoritative after data changes.

## Architecture

The application uses a layered structure:

```text
HTTP request
    │
    ▼
Controller ── validates and maps HTTP input
    │
    ▼
Service interface / implementation ── business rules and transactions
    │
    ▼
Repository ── JPA persistence, native queries, and database locks
    │
    ▼
H2 database
```

### Layer responsibilities

| Layer | Responsibility |
| --- | --- |
| Controller | Routes, headers, path/query parameters, request bodies, and HTTP status codes |
| DTO | Validated request contracts and API response shapes |
| Service | Role checks, ownership checks, workflows, transaction boundaries, and state changes |
| Repository | Entity persistence, pagination, visibility queries, and row locking |
| Model | JPA entities, relationships, constraints, and order status rules |
| Config | Demo-data initialization, scheduled notification dispatch, and OpenAPI metadata |
| Common | Shared access checks and consistent API error responses |

Spring-managed dependencies use explicit field-level `@Autowired` injection. Repository and service fields use the lower-camel-case form of their type, such as `cityRepository` and `userService`.

## Data model

| Entity | Purpose | Important relationships |
| --- | --- | --- |
| `User` | Stores username, full name, email, phone number, and role | Owns restaurants, places orders, or backs a partner profile |
| `City` | Service location | Contains restaurants and delivery partners |
| `Restaurant` | Restaurant profile | Belongs to a city and an owner |
| `MenuItem` | Sellable restaurant item | Belongs to one restaurant and tracks price, stock, and availability |
| `CustomerOrder` | Customer purchase | Belongs to a customer and restaurant; may have one partner |
| `OrderItem` | Historical order line | Stores item name, unit price, and quantity at purchase time |
| `Payment` | Simulated payment state | One payment per order |
| `DeliveryPartner` | Delivery profile | Connects a partner user to a city and optional active order |
| `OrderEvent` | Audit and notification event | Records order status, actor, recipients, and dispatch state |
| `Notification` | User inbox entry | Connects an event to one recipient |
| `Review` | Delivered-order feedback | One review per order |

Historical order lines copy the item name and price so later menu changes do not rewrite past orders.

## Order lifecycle

The standard successful flow is:

```text
PLACED → ACCEPTED → PREPARING → OUT_FOR_DELIVERY → DELIVERED
```

Alternative final transitions are:

```text
PLACED → REJECTED
PLACED → CANCELLED
```

| Target status | Required actor |
| --- | --- |
| `ACCEPTED` | Restaurant owner |
| `PREPARING` | Restaurant owner |
| `OUT_FOR_DELIVERY` | Assigned delivery partner |
| `DELIVERED` | Assigned delivery partner |
| `REJECTED` | Restaurant owner |
| `CANCELLED` | Customer who placed the order |

A partner may claim an order only while it is `ACCEPTED` or `PREPARING`.

## API reference

All application endpoints start with `/api`.

Paginated endpoints accept:

- `page`: zero-based page number, default `0`, maximum `100000`
- `size`: page size, default `20`, allowed range `1` to `100`

Paginated responses contain `content`, `page`, `size`, and `totalElements`.

### Users

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/customers` | Public | Register a customer |
| `GET` | `/api/me` | `X-User-Id` | Return the selected user's profile |
| `POST` | `/api/admin/users` | Admin | Create a user with a selected role |

### Catalog

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/cities` | Public | List cities |
| `POST` | `/api/admin/cities` | Admin | Create a city |
| `PUT` | `/api/admin/cities/{id}` | Admin | Update a city |
| `GET` | `/api/restaurants` | Public | List restaurants; optionally filter by `cityId` and search name with `q` |
| `GET` | `/api/restaurants/{id}` | Public | Get one restaurant |
| `POST` | `/api/admin/restaurants` | Admin | Create a restaurant |
| `PUT` | `/api/admin/restaurants/{id}` | Admin | Update restaurant name, address, and active state |
| `PUT` | `/api/restaurants/{id}/opening-hours` | Owner or admin | Set or clear a restaurant's daily opening hours |
| `GET` | `/api/restaurants/{restaurantId}/menu` | Public | List a restaurant's menu; search name/description with `q` |
| `POST` | `/api/restaurants/{restaurantId}/menu` | Owner or admin | Create a menu item |
| `PUT` | `/api/restaurants/{restaurantId}/menu/{id}` | Owner or admin | Update a menu item |
| `POST` | `/api/restaurants/{restaurantId}/menu/{id}/stock` | Owner or admin | Apply a signed stock adjustment |

### Delivery partners

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/admin/partners` | Admin | Create a delivery-partner profile |
| `PUT` | `/api/admin/partners/{id}` | Admin | Change partner city or active state |
| `GET` | `/api/admin/partners` | Admin | List delivery-partner profiles |
| `GET` | `/api/delivery/me` | Partner | Get the current partner profile |
| `GET` | `/api/delivery/orders/available` | Partner | List claimable orders in the partner's city |

### Orders

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/orders` | Customer | Place an order; requires `Idempotency-Key` |
| `GET` | `/api/orders` | Authenticated role | List visible orders; optionally filter by `status` |
| `GET` | `/api/orders/{id}` | Authorized participant or admin | Get order details, payment, lines, and history |
| `PATCH` | `/api/orders/{id}/status` | Authorized participant | Change order status |
| `POST` | `/api/orders/{id}/claim` | Partner | Claim an accepted or preparing order |

### Reviews and notifications

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/orders/{id}/review` | Purchasing customer | Review a delivered order |
| `GET` | `/api/restaurants/{id}/reviews` | Public | List reviews and average rating |
| `GET` | `/api/notifications` | `X-User-Id` | List the selected user's notifications |

## Search and opening hours

### Search

```http
GET /api/restaurants?cityId=1&q=spice&page=0&size=10
GET /api/restaurants/1/menu?q=rice&page=0&size=10
```

`q` performs a case-insensitive substring search. Restaurant search matches the name; menu search matches the name or description within the selected restaurant. Surrounding whitespace is trimmed, and a missing or blank query returns the normal listing. Queries are limited to 150 characters. Characters such as `%` and `_` are literal text, not wildcards. Native SQL applies the same filters to the result and pagination count.

### Opening hours

An owner can update their restaurant's hours; an admin can update any restaurant:

```http
PUT /api/restaurants/1/opening-hours
X-User-Id: 2
Content-Type: application/json

{
    "opensAt": "09:00",
    "closesAt": "22:00"
}
```

- Times use ISO local-time strings, such as `09:00` or `22:00:00`, in the configured restaurant timezone. The default is `Asia/Kolkata`, independent of the server's timezone.
- The same window applies every day. Opening time is inclusive and closing time is exclusive.
- Overnight windows are supported: `18:00` to `02:00` allows orders in the evening and after midnight until 02:00.
- Both times must be supplied together and must differ. Set both to `null` to restore all-day opening. New and existing restaurants without configured hours keep their all-day behavior.
- Restaurant responses include `opensAt`, `closesAt`, `timeZone`, and `openNow`. Inactive restaurants or cities always have `openNow: false`. Search results still include closed restaurants so their schedules remain visible.
- Checkout checks opening hours before reserving stock or creating a payment and returns HTTP `409` while closed. Existing orders can still progress, and replaying a successful idempotency key returns the original order even after closing.

## Example order flow

Additional ready-to-run requests are available in [`docs/demo.http`](docs/demo.http).

### 1. Place an order

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'X-User-Id: 4' \
  -H 'Idempotency-Key: lunch-001' \
  -H 'Content-Type: application/json' \
  -d '{
    "restaurantId": 1,
    "items": [{"menuItemId": 1, "quantity": 2}],
    "deliveryAddress": "42 Lake Road, Mumbai",
    "paymentToken": "TEST_SUCCESS"
  }'
```

`TEST_SUCCESS` captures a simulated payment. `TEST_DECLINE` returns HTTP `402` and rolls back the order.

### 2. Accept and prepare the order

```bash
curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H 'X-User-Id: 2' \
  -H 'Content-Type: application/json' \
  -d '{"status":"ACCEPTED"}'

curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H 'X-User-Id: 2' \
  -H 'Content-Type: application/json' \
  -d '{"status":"PREPARING"}'
```

### 3. Claim and deliver the order

```bash
curl -X POST http://localhost:8080/api/orders/1/claim \
  -H 'X-User-Id: 6'

curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H 'X-User-Id: 6' \
  -H 'Content-Type: application/json' \
  -d '{"status":"OUT_FOR_DELIVERY"}'

curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H 'X-User-Id: 6' \
  -H 'Content-Type: application/json' \
  -d '{"status":"DELIVERED"}'
```

### 4. Review the delivered order

```bash
curl -X POST http://localhost:8080/api/orders/1/review \
  -H 'X-User-Id: 4' \
  -H 'Content-Type: application/json' \
  -d '{"rating":5,"comment":"Arrived warm and on time"}'
```

## Business rules

### Ordering and stock

- An order contains items from exactly one restaurant.
- A basket supports up to 50 distinct menu items.
- Each line quantity must be between 1 and 100.
- Duplicate menu-item lines are rejected.
- Stock is reserved inside the order transaction.
- Menu items are locked in ascending ID order to reduce deadlock risk.
- Rejected and cancelled orders restore their reserved stock.
- Inactive cities, inactive restaurants, closed opening hours, unavailable items, and insufficient stock block new orders.

### Idempotency

- `Idempotency-Key` is required when placing an order.
- Keys are scoped to the customer.
- Repeating the same normalized request returns the existing order with HTTP `200` and `Idempotency-Replayed: true`.
- Reusing the key for a different request returns HTTP `409`.
- A newly created order returns HTTP `201` and `Idempotency-Replayed: false`.

### Delivery assignment

- A partner can hold at most one active order.
- An order can have at most one delivery partner.
- The partner and restaurant must belong to the same city.
- Inactive partners cannot claim orders.
- A busy partner cannot be moved to another city or deactivated.
- Assignment-changing flows lock the order before the partner.

### Notifications

- Each order change creates an `OrderEvent` in the same transaction as the business change.
- A scheduled worker converts pending events into inbox notifications.
- Customer and restaurant owner receive order notifications; the assigned partner is included when present.
- Notification creation and event acknowledgement commit together.

### Reviews

- Only the customer who placed the order may review it.
- The order must be `DELIVERED`.
- An order can be reviewed once.
- Ratings must be between 1 and 5.

## Errors and validation

Errors use this response shape:

```json
{
  "timestamp": "2026-09-26T10:15:30Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Input validation failed",
  "fields": {
    "username": "must match the required pattern"
  }
}
```

| Status | Meaning |
| --- | --- |
| `400` | Invalid input, malformed JSON, invalid transition target, or missing header |
| `402` | Simulated payment declined |
| `403` | Role or ownership check failed |
| `404` | Entity or endpoint not found |
| `405` | HTTP method not supported |
| `409` | Duplicate data, restaurant closed, stock conflict, lifecycle conflict, assignment conflict, or concurrent update |
| `415` | Unsupported content type |
| `500` | Unexpected server error |

Unknown JSON fields are rejected. Usernames, text lengths, prices, quantities, stock changes, ratings, and pagination values are validated before service processing.

## Configuration

Configuration lives in [`src/main/resources/application.properties`](src/main/resources/application.properties).

| Property or environment variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:h2:file:./data/food-delivery;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000` | JDBC connection URL |
| `DB_USERNAME` | `sa` | Database username |
| `DB_PASSWORD` | Empty | Database password |
| `DEMO_DATA` | `true` | Seed demo data when the users table is empty |
| `server.port` | `8080` | HTTP port |
| `app.restaurant.time-zone` / `RESTAURANT_TIME_ZONE` | `Asia/Kolkata` | Timezone shared by all restaurant schedules |
| `app.notifications.enabled` | `true` | Enable the scheduled notification worker |
| `app.notifications.initial-delay-ms` | `5000` | Delay before the first notification scan |
| `app.notifications.delay-ms` | `500` | Delay between notification scans |
| `app.async.core-pool-size` | `2` | Core notification worker threads |
| `app.async.max-pool-size` | `4` | Maximum notification worker threads |
| `app.async.queue-capacity` | `100` | Queued notification tasks before caller-runs backpressure |
| `spring.jpa.hibernate.ddl-auto` | `update` | Create or update the database schema |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | Swagger UI path |
| `springdoc.api-docs.path` | `/v3/api-docs` | OpenAPI JSON path |

Example overrides:

```bash
DB_URL='jdbc:h2:mem:food-delivery' \
DEMO_DATA=false \
mvn spring-boot:run
```

To reset the default persistent database, stop the application and remove only the `data/food-delivery*` files.

## Project structure

```text
src/main/java/com/dmg/fooddelivery/
├── FoodDeliveryApplication.java
├── common/          access checks and API error handling
├── config/          demo data, notification worker, and OpenAPI metadata
├── controller/      HTTP endpoints
├── dto/             request and response records
├── model/           JPA entities and enums
├── repository/      Spring Data repositories and native SQL queries
└── service/
    ├── *.java       service contracts
    └── impl/        business logic and transaction boundaries
```

Useful supporting files:

- [`docs/demo.http`](docs/demo.http): manual API requests
- [`docs/verification.md`](docs/verification.md): recorded build and manual verification notes
- [`docs/walkthrough.md`](docs/walkthrough.md): implementation walkthrough outline
- [`AGENTS.md`](AGENTS.md): project conventions for future changes

## Scope and limitations

- Payment is simulated locally; no money is charged.
- `X-User-Id` is not authentication.
- Notifications are stored in the database; there is no email, SMS, push provider, or message broker.
- Delivery addresses are free text; there is no mapping, distance, or geocoding integration.
- Currency is fixed to INR.
- Opening hours use one daily window and one application-wide timezone; weekly schedules, split shifts, and holiday overrides are not implemented.
- Taxes, discounts, fees, tips, partner reassignment, review editing, and account deletion are not implemented.
- The embedded H2 database and `ddl-auto=update` are suitable for this assignment, not a production rollout.
- The application is designed as one process and does not claim multi-node coordination.

## Verification

Run the complete integration suite with:

```bash
mvn test
```

The test suite currently contains 97 passing test cases:

- 88 unit-test cases covering authorization, every valid and invalid order transition, transition roles, restaurant daytime and overnight hours, stock adjustment boundaries, user normalization and duplicate detection, and review rules.
- 9 integration-test cases covering every controller route, full order lifecycles, validation and rollback paths, asynchronous notification delivery, idempotency, stock restoration, and real concurrent requests for stock reservation and delivery-partner claims.

Unit tests run without a Spring context. Integration tests use isolated in-memory H2 databases and do not touch the normal `./data` database.

To compile and package without executing the suite:

```bash
mvn -DskipTests clean package
```
