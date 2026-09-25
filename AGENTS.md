# Global testing policy

- Do not create, write, generate, modify, or run automated tests unless the user explicitly requests tests in the current task.
- This prohibition includes unit, integration, end-to-end, acceptance, regression, snapshot, visual-regression, browser, and UI tests.
- Do not create or run test scripts or harnesses in any language or file format, including `.mjs`, `.js`, `.ts`, `.tsx`, `.jsx`, `.py`, and shell scripts.
- Do not invoke test runners or test-oriented commands such as Jest, Vitest, Mocha, Playwright, Cypress, Testing Library, pytest, unittest, XCTest, Espresso, or equivalent tools.
- When implementing or changing code, verify it with non-test methods where practical, such as static inspection, type checking, linting, compilation, builds, or narrowly scoped manual checks that do not create or execute automated test code.
- If repository instructions or a task workflow normally calls for automated tests, skip that step and state clearly in the final response that tests were not written or run because of this global policy.
- An explicit request from the user to write or run tests in the current task overrides this policy only for that task and only for the requested test scope.

## Authorization for this implementation

The user initially requested tests, then explicitly instructed: "do not write any tests for now". The latest instruction supersedes the earlier authorization. Do not write or run automated tests for this implementation; use compilation, builds, static review, and narrow manual API checks.

## Project conventions

- Java 17 / Spring Boot / Spring Data JPA / H2; no Spring Security.
- Use model, repository, controller, service, and service/impl packages.
- Keep transactions in service implementations, HTTP mapping in controllers, and persistence in JPA repositories.
- Use the X-User-Id demo header with simple database-backed role and ownership checks.
- Preserve database locking and lock ordering when changing stock, assignments, or lifecycle code.
- Do not claim concurrency has been verified by tests when only static inspection or manual checks have been performed.
- Keep assumptions and runnable examples in README.md. Do not add frontend, deployment, or CI tooling.
