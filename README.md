# Food Delivery Order Management

A Spring Boot REST backend for the supplied [problem statement](Food%20Delivery%20Order%20Management.pdf). It supports cities, restaurants, menus, stock, customer orders, delivery claims, status notifications, and reviews.

The implementation follows the requested **model → repository → service / service impl → controller** structure. It uses **Spring Data JPA and H2**, with **no Spring Security**. There is no frontend, deployment setup, or microservice infrastructure.

## Run locally

Use Java 17 and Maven 3.6.3 or newer. Spring Boot is pinned to 3.5.16; see its [Java compatibility requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html). Ensure `mvn -version` reports a supported JDK; Java 17 is the verified version.

```bash
mvn -DskipTests package
java -jar target/food-delivery-1.0.0.jar
```

Alternatively:

```bash
mvn spring-boot:run
```

The API runs at `http://localhost:8080`. H2 stores data in `./data/food-delivery.mv.db`, so orders survive application restarts. Hibernate creates/updates the schema from the JPA models. No database installation is needed.

Configuration is defined in `src/main/resources/application.properties`:

| Setting | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:h2:file:./data/food-delivery;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000` | H2 database location |
| `DB_USERNAME` | `sa` | Database username |
| `DB_PASSWORD` | empty | Database password |
| `DEMO_DATA` | `true` | Seed example data when the users table is empty |
| `--server.port=...` | `8080` | HTTP port |
| `--app.notifications.delay-ms=...` | `500` | Background notification polling interval |

To start a disposable database, pass `--spring.datasource.url=jdbc:h2:mem:food-delivery`. To reset persistent demo data, stop the app and delete only its `data/food-delivery*` files. Leave demo seeding enabled for the first launch to create the initial admin. Disabling seeding does not remove existing data.

## Demo users and role checks

Send `X-User-Id` on write endpoints and private read endpoints. The service loads that user from H2 and checks their stored role and resource ownership. A client cannot pass a role in the header. Public catalog/review browsing and customer registration do not require this header.

This header is a **demo identity selector, not authentication**: a caller can select another known user ID. Passwords, login, tokens, and Spring Security were deliberately removed at the user's request. The role checks demonstrate the required business rules, but do not verify the caller's real identity.

Fresh database seed:

| User ID | Username | Role | Scope |
| --- | --- | --- | --- |
| 1 | admin | ADMIN | Manage users, cities, restaurants, partners; view orders |
| 2 | owner | OWNER | Spice Kitchen, restaurant 1, Mumbai |
| 3 | owner2 | OWNER | Garden Cafe, restaurant 2, Pune |
| 4 | customer | CUSTOMER | Place, track, cancel, and review own orders |
| 5 | customer2 | CUSTOMER | Separate customer for ownership demonstrations |
| 6 | partner | PARTNER | Delivery partner profile 1, Mumbai |
| 7 | partner2 | PARTNER | Delivery partner profile 2, Mumbai |

Menu items: `1` Paneer Bowl (INR 180, stock 20), `2` Rice Bowl (INR 120, stock 10), and `3` Pasta at Garden Cafe (INR 220, stock 15). Cities are Mumbai `1` and Pune `2`. These IDs apply to a fresh database; response IDs are authoritative after data changes.

## Example order flow

[docs/demo.http](docs/demo.http) contains individual requests you can send manually from an HTTP client. It contains no test scripts or assertions.

Place an order as customer 4:

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'X-User-Id: 4' \
  -H 'Idempotency-Key: lunch-001' \
  -H 'Content-Type: application/json' \
  -d '{"restaurantId":1,"items":[{"menuItemId":1,"quantity":2}],"deliveryAddress":"42 Lake Road, Mumbai","paymentToken":"TEST_SUCCESS"}'
```

Use the returned order ID in subsequent requests. For a fresh database it is `1`:

```bash
# Restaurant owner accepts.
curl -X PATCH http://localhost:8080/api/orders/1/status -H 'X-User-Id: 2' -H 'Content-Type: application/json' -d '{"status":"ACCEPTED"}'

# Partner claims the order.
curl -X POST http://localhost:8080/api/orders/1/claim -H 'X-User-Id: 6'

# Restaurant owner starts preparation.
curl -X PATCH http://localhost:8080/api/orders/1/status -H 'X-User-Id: 2' -H 'Content-Type: application/json' -d '{"status":"PREPARING"}'

# Assigned partner picks up and delivers.
curl -X PATCH http://localhost:8080/api/orders/1/status -H 'X-User-Id: 6' -H 'Content-Type: application/json' -d '{"status":"OUT_FOR_DELIVERY"}'
curl -X PATCH http://localhost:8080/api/orders/1/status -H 'X-User-Id: 6' -H 'Content-Type: application/json' -d '{"status":"DELIVERED"}'

# Customer reviews the delivered order and reads their notifications.
curl -X POST http://localhost:8080/api/orders/1/review -H 'X-User-Id: 4' -H 'Content-Type: application/json' -d '{"rating":5,"comment":"Arrived warm and on time"}'
curl http://localhost:8080/api/notifications -H 'X-User-Id: 4'
```

## APIs

All paths below are under `/api`. Lists accept `page` (zero-based, default 0) and `size` (1–100, default 20), sort by descending ID, and return `content`, `page`, `size`, and `totalElements`.

| Method | Path | Access / purpose |
| --- | --- | --- |
| POST | `/customers` | Public; create a customer with `username` |
| GET | `/me` | Selected user's ID, username, and role |
| POST | `/admin/users` | Admin; create `username` and `role` |
| GET | `/cities` | Public; list cities |
| POST / PUT | `/admin/cities` / `/admin/cities/{id}` | Admin; `name`, `active` |
| GET | `/restaurants?cityId=1` | Public; optional city filter |
| GET | `/restaurants/{id}` | Public; restaurant details |
| POST | `/admin/restaurants` | Admin; `cityId`, `ownerId`, `name`, `address`, `active` |
| PUT | `/admin/restaurants/{id}` | Admin; update `name`, `address`, `active` |
| GET | `/restaurants/{id}/menu` | Public; menu and stock |
| POST | `/restaurants/{id}/menu` | Owner of restaurant or admin; `name`, `description`, `price`, `stock`, `available` |
| PUT | `/restaurants/{id}/menu/{itemId}` | Owner or admin; `name`, `description`, `price`, `available` |
| POST | `/restaurants/{id}/menu/{itemId}/stock` | Owner or admin; signed, nonzero `delta` |
| POST / GET | `/admin/partners` | Admin; create (`userId`, `cityId`, `active`) or list profiles |
| PUT | `/admin/partners/{id}` | Admin; `cityId`, `active` |
| GET | `/delivery/me` | Partner; own profile and active order |
| GET | `/delivery/orders/available` | Partner; claimable orders in own city |
| POST | `/orders` | Customer; basket, address, simulated payment, and `Idempotency-Key` |
| GET | `/orders?status=PLACED` | Role-scoped list; optional status filter |
| GET | `/orders/{id}` | Customer, owner, assigned partner, or admin with access; includes payment and history |
| PATCH | `/orders/{id}/status` | Authorized participant; `status` |
| POST | `/orders/{id}/claim` | Partner; claim an accepted/preparing order |
| POST | `/orders/{id}/review` | Customer who placed a delivered order; `rating`, `comment` |
| GET | `/restaurants/{id}/reviews` | Public; reviews, count, and average rating |
| GET | `/notifications` | Selected user's notification inbox |

The `X-User-Id` header is required unless access is marked public. Resource IDs are distinct: a user's ID is not their delivery partner profile's ID.

Request validation rejects missing required values, invalid enums, unknown JSON properties, negative quantities/prices, oversized baskets/text, and unsupported pagination. Monetary input permits at most two decimal places. Error responses have `timestamp`, `status`, `code`, `message`, and `fields`.

| HTTP status | Meaning |
| --- | --- |
| 200 / 201 | Success / resource created |
| 400 | Invalid input, missing header, or malformed JSON |
| 402 | Simulated payment declined |
| 403 | Wrong role or resource ownership |
| 404 | Resource not found |
| 409 | Stock, state, assignment, duplicate, or concurrency conflict |
| 415 | Unsupported content type |

## Structure

```text
src/main/java/com/dmg/fooddelivery/
├── model/          JPA entities, roles, order lifecycle
├── repository/     Spring Data JpaRepository interfaces and locking queries
├── controller/     REST endpoints and request validation
├── service/        Service interfaces
│   └── impl/       Business logic and transaction boundaries
├── dto/            Request/response records and pagination
├── common/         Role/ownership checks and consistent API errors
└── config/         Demo seeding and notification worker
```

Controllers depend on service interfaces. Implementations use constructor injection and JPA repositories. Lombok removes entity getter/setter and constructor boilerplate. DTOs prevent persistence relationships from leaking into JSON. `open-in-view=false` keeps data loading inside service transactions.

Java code uses explicit types and imports, four-space indentation, braces for control flow, and one statement per line. `.editorconfig` and `AGENTS.md` record these conventions for future changes.

## Rules, assumptions, and correctness

- **One restaurant per order.** Up to 50 distinct items, 1–100 units each. Duplicate lines are rejected rather than silently combined. Price and item name are copied into order items so future menu edits do not change old orders.
- **Stock means units available to sell.** Placement deducts stock. Rejection/cancellation returns it. Delivery does not deduct it again. Initial stock and each stock adjustment are bounded to one million units; accumulated stock uses a nonnegative `long` so a later refund can restore units even after restocking.
- **Atomic checkout.** The order, its items, captured payment record, stock changes, and pending event share one `@Transactional` operation. Any exception rolls them back together. There are no real charges: `TEST_SUCCESS` captures locally and `TEST_DECLINE` returns 402. A real external payment provider cannot be made atomic with H2 merely by adding `@Transactional`; it would require a different integration design.
- **Stock contention.** `@Lock(PESSIMISTIC_WRITE)` locks each menu item until commit. Basket items are locked in ascending ID order. An H2 check constraint also prevents negative stock. Menu updates and stock adjustments acquire the same item lock.
- **Safe retries.** `Idempotency-Key` is required (1–80 letters, digits, `.`, `_`, `:`, or `-`) and scoped to the customer. A customer-row lock serializes the first use of a key. A unique database constraint backs it up. The same normalized request returns the existing order with 200 and `Idempotency-Replayed: true`; a different request with that key returns 409. A failed placement does not retain its key. Keys do not expire in this implementation.
- **Lifecycle.** `PLACED → ACCEPTED → PREPARING → OUT_FOR_DELIVERY → DELIVERED`. The restaurant owner accepts and starts preparation. Only the assigned partner performs pickup and delivery. Only `PLACED` orders may become `REJECTED` (owner) or `CANCELLED` (customer); both atomically restore stock and refund the simulated payment. These final states cannot be reopened. Authorized repeats of the current status have no extra side effects.
- **Assignment.** A partner claims an accepted/preparing order in their own city. One partner has at most one active order; one order has at most one partner. Claiming locks the order first, then the partner. Delivery follows the same lock order and releases the partner. Repeating the winning claim is harmless. Relocation/deactivation of a busy partner is rejected.
- **Asynchronous notifications.** Order events are a durable queue as well as the audit trail. A separate scheduled thread processes up to 100 pending events every 500 ms, writing inbox notifications for the customer, restaurant owner, and assigned partner. Recipients and status are captured when the event is created. Events before assignment have no partner recipient. Request threads only enqueue; they do not deliver notifications. Delivery and acknowledgement share a transaction, failed deliveries remain pending, and event/recipient uniqueness prevents duplicates. This demonstrates local inbox delivery, not email/SMS/push or a message broker.
- **Reviews.** Only the purchasing customer can review a delivered order, once, with rating 1–5 and an optional empty comment. A locked order and a unique order/review constraint prevent duplicates. The average is `null` when no reviews exist.
- **Scope and access.** Admins manage cities, restaurants, accounts, and delivery profiles; owners manage their own menus/orders. Admins can read all orders but do not impersonate owners or partners for lifecycle updates. Other roles can read only their own orders. Available delivery listings omit customer addresses; the assigned partner can see delivery details.
- **Soft deactivation.** Cities/restaurants use `active`; menu items use `available`. Historical data is preserved. Catalog listings include inactive/unavailable entries with their flags. Deactivation prevents new orders validated after the change; an already validated checkout may finish. Existing orders can still progress. Restaurant city/owner are immutable through the API. Delivery addresses are free text and assumed to be in the restaurant's city; geocoding is out of scope.
- **Other boundaries.** Currency is INR; money uses `BigDecimal`. No taxes, discounts, fees, real refunds, partner reassignment, account deletion, password management, or review editing. One Spring Boot process and embedded H2 are intended for this assignment, not a multi-node deployment. Hibernate `ddl-auto=update` keeps setup simple; database migrations and production capacity tuning are deferred.

## Verification and submission

Automated tests were **not written or run**, following the user's latest instruction. Compilation, JAR packaging, and narrow manual API checks are used instead. See [docs/verification.md](docs/verification.md) for the observed results and limits. Concurrent-request correctness has been reviewed in code, not stress-tested.

The repository includes the original PDF, the development instructions in [AGENTS.md](AGENTS.md), the [PDF skill used to read the assignment](docs/skills/pdf/SKILL.md), and an [AI workflow / recording outline](docs/walkthrough.md). The local Git history preserves implementation and simplification commits. Publishing a personal GitHub repository and recording the requested video remain submission steps for the owner; no remote repository or video has been created.
