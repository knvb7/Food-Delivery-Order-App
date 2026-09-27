## Project conventions

- Java 17 / Spring Boot / Spring Data JPA / H2; no Spring Security.
- Use model, repository, controller, service, and service/impl packages.
- Use explicit `@Autowired` field injection for Spring-managed dependencies.
- Use explicit Java types; do not use `var`.
- Use four-space indentation, explicit imports, braces for control flow, and one statement per line.
- Separate annotations, fields, methods, and logical blocks with readable line breaks and spacing.
- Prefer straightforward loops and named intermediate values over deeply nested stream or builder chains.
- Keep public service flows easy to follow; use small private helpers for substantial steps without adding unnecessary layers.
- Keep application configuration in `src/main/resources/application.properties`, not YAML.
- Keep transactions in service implementations, HTTP mapping in controllers, and persistence in JPA repositories.
- Write custom repository queries as native SQL with `@Query(nativeQuery = true)` and explicit `@Param` bindings.
- Use SQL `FOR UPDATE` for native locking queries, and provide matching native `countQuery` statements for paginated queries.
- Use the X-User-Id demo header with simple database-backed role and ownership checks.
- Preserve database locking and lock ordering when changing stock, assignments, or lifecycle code.
- Do not claim concurrency has been verified by tests when only static inspection or manual checks have been performed.
- Keep assumptions and runnable examples in README.md. Do not add frontend, deployment, or CI tooling.
