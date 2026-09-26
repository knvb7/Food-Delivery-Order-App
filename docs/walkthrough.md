# Recording outline (under 10 minutes)

1. **Problem and scope, 0:00–1:00.** Show the original PDF. Explain restaurants across cities, inventory, atomic checkout, lifecycle, delivery contention, notifications, and reviews. State that the owner requested a simpler JPA/H2 structure, no Spring Security, and no tests for now.
2. **Project layout, 1:00–2:00.** Open `model`, `repository`, `controller`, `service`, and `service/impl`. Explain DTOs, `@Autowired` field injection, H2 persistence, and why no frontend or deployment infrastructure was added.
3. **Manual API demonstration, 2:00–5:00.** Start the application. Use `demo.http` to browse, place, accept, claim, prepare, deliver, and review. Show the durable history and notification inbox. Explain that `X-User-Id` selects a demo identity and does not authenticate a real caller.
4. **Transactions and contention, 5:00–7:00.** Show `OrderServiceImpl.place`, repository pessimistic locks, deterministic item lock ordering, the unique idempotency key, and order-then-partner lock ordering. Demonstrate a declined payment and explain the local payment assumption.
5. **Asynchronous delivery, 7:00–8:00.** Show the event model and scheduled notification worker. Explain recipient snapshots, rollback/retry, and idempotent inbox delivery.
6. **AI workflow and verification, 8:00–9:00.** The assistant read the PDF using the checked-in PDF skill, implemented the backend, and then refactored it based on user feedback. The original JDBC/Spring Security approach was replaced by JPA, service interfaces, and simpler demo role checks. Builds and individual manual HTTP requests were used for verification. No automated tests were written or run following the latest instruction.
7. **Assumptions and remaining work, 9:00–9:45.** Point to README rules. Explain simulated payments, embedded H2, no real authentication, and the lack of automated concurrency verification. The assignment's test deliverable is deferred by explicit user request. Publish the GitHub repository and attach the recording link separately.

## Development artifacts

- Source requirement: `Food Delivery Order Management.pdf` (preserved unchanged).
- Local development instructions: `AGENTS.md`.
- Skill used: `docs/skills/pdf/SKILL.md` (copied from the PDF skill read during development).
- Source code: `src/main`, `pom.xml`.
- Manual examples: `docs/demo.http`.
- Verification evidence summary: `docs/verification.md`.

This file is a recording guide, not a claim that a video or remote repository has been created.
