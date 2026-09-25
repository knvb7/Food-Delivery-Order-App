# Global testing policy

- Do not create, write, generate, modify, or run automated tests unless the user explicitly requests tests in the current task.
- This prohibition includes unit, integration, end-to-end, acceptance, regression, snapshot, visual-regression, browser, and UI tests.
- Do not create or run test scripts or harnesses in any language or file format, including `.mjs`, `.js`, `.ts`, `.tsx`, `.jsx`, `.py`, and shell scripts.
- Do not invoke test runners or test-oriented commands such as Jest, Vitest, Mocha, Playwright, Cypress, Testing Library, pytest, unittest, XCTest, Espresso, or equivalent tools.
- When implementing or changing code, verify it with non-test methods where practical, such as static inspection, type checking, linting, compilation, builds, or narrowly scoped manual checks that do not create or execute automated test code.
- If repository instructions or a task workflow normally calls for automated tests, skip that step and state clearly in the final response that tests were not written or run because of this global policy.
- An explicit request from the user to write or run tests in the current task overrides this policy only for that task and only for the requested test scope.

## Authorization for this implementation

The user explicitly requested: "hey please read the problem statement and implement a good solution for it. Implement the test cases as well please". Unit and integration tests for this solution are therefore authorized.

## Project conventions

- Java 17 / Spring Boot; one application and one relational database.
- Keep transactions in services, HTTP mapping in controllers, and SQL access in repositories.
- Preserve database locking and lock ordering when changing stock, assignments, or lifecycle code.
- Never replace real concurrency tests with mocked repository tests.
- Keep assumptions and runnable examples in README.md. Do not add frontend, deployment, or CI tooling.
