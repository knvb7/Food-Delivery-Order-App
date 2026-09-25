# Verification record

Date: 2026-09-25. Environment: Java 17.0.19, Maven 3.9.16, Spring Boot 3.5.16, embedded H2. Manual API checks ran the packaged application on port 18080 against a separate file database under `/tmp`, leaving the normal `./data` database untouched.

No automated tests or test harnesses were created or run. The user explicitly postponed tests. The observations below come from compilation, packaging, static inspection, and individual manual HTTP requests.

## Build and structure

- `mvn clean compile`: successful.
- `mvn -DskipTests package`: successful; 54 Java source files compiled and the executable JAR was produced. Maven reported tests skipped and no test sources.
- Packaged application started successfully, with nine Spring Data JPA repositories discovered.
- No Spring Security, handwritten `JdbcTemplate`, Flyway, or test dependencies remain in the final application.
- `git diff --check`: clean.

## Observed API behavior

| Manual action | Observed result |
| --- | --- |
| Browse restaurants without an identity header | 200; both seeded restaurants returned |
| Customer creates an order for two Paneer Bowls | 201; total INR 360, `PLACED`, captured payment, event history, stock 20 → 18 |
| Customer calls an admin city-creation endpoint | 403 |
| Owner accepts, partner claims, owner prepares | All returned 200 with the expected state/partner/history |
| Assigned partner marks out-for-delivery, then delivered | 200; order reached `DELIVERED` |
| Read partner profile after delivery | `activeOrderId` was null |
| Purchasing customer submits a review | 201; rating 5 persisted |
| Browse restaurant reviews | 200; average rating 5.00 and review returned |
| Read partner notification inbox | Assignment and subsequent state events appeared asynchronously |
| Place another order with `TEST_DECLINE` | 402; stock remained 18 and customer order count remained one |
| A different customer reads the delivered order | 403 |
| Replay the original successful request and key | 200, `Idempotency-Replayed: true`, same order and payment reference |
| Place a separate Rice Bowl order, then cancel while placed | 200; `CANCELLED`, payment `REFUNDED`, stock restored to 10 |
| Submit quantity zero | 400 with an `items[0].quantity` validation error |
| Stop and restart against the same H2 file | Delivered/cancelled orders and the review remained present |

## Limits

Concurrency guarantees were inspected in the code: pessimistic item locks, deterministic basket lock order, order/partner lock order, customer-scoped idempotency locking, and database uniqueness/check constraints. No concurrent-request, load, failure-injection, unit, integration, or other automated tests were performed. The manual observations do not establish stress behavior or exhaustive endpoint coverage.

The identity header and local payment ledger are explicit assignment simplifications, not production authentication or a real payment integration.

## Formatting and properties follow-up

The later readability update replaced local `var` declarations and wildcard imports with explicit types/imports, expanded compact statements and control flow, and formatted all Java sources, the Maven POM, and manual request bodies. Configuration moved from `application.yml` to `application.properties` with the same settings and environment defaults.

After this update, `mvn clean compile` compiled all 54 Java files successfully. A manual launch on port 18081 with a disposable H2 database started successfully, and a catalog request returned HTTP 200 with the seeded restaurants. Source inspection found no remaining `var` declarations or wildcard imports; `git diff --check` was clean. No automated tests were written or run.

## Native SQL follow-up

All custom repository queries, including previously derived finders, now declare `@Query(nativeQuery = true)`. Named parameters use explicit `@Param` bindings. Paginated finders include matching native count queries, and native locking queries use `FOR UPDATE` in place of JPA `@Lock`. The order status filter is bound as its stored string name. Standard inherited JPA persistence operations remain unchanged.

`mvn clean compile` succeeded after the change. Individual manual requests against a disposable H2 database on port 18082 confirmed:

- Restaurant/menu pagination and city filtering, with correct counts using page size 1.
- Order placement, status filtering, admin/customer visibility, and an empty result for another customer.
- Owner acceptance, available-order listing, partner claim, preparation, pickup, delivery, and partner release.
- Review creation, native average-rating calculation, paginated reviews, and notification inbox retrieval.

These checks exercise native SQL syntax and mappings through the running application. No automated tests or concurrent-request checks were written or run.
