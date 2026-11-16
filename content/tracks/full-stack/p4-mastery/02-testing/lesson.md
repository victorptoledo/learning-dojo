# Testing Across the Stack: Unit, Contract, Integration and End-to-End

Each language track teaches its test framework. The full stack question is different: across a
UI, an API and a database, **which tests buy the most confidence per minute of CI?**

## What each level proves

| Level | Proves | Typical tools | Speed |
| --- | --- | --- | --- |
| **Static** | types line up, obvious bugs | `tsc`, ESLint, the Java compiler, SpotBugs | instant |
| **Unit** | a function or class does its job | Vitest, Jest, node:test, JUnit, xUnit, pytest | milliseconds |
| **Integration** | your code works with real infrastructure (database, queue, framework) | Testcontainers, `@SpringBootTest`, MockMvc, Supertest | seconds |
| **Component** (UI) | a component renders states and reacts to the user | Testing Library, Vitest browser mode, Playwright component tests | fast |
| **Contract** | client and server agree on the API | generated types, schema validation, Pact | fast |
| **End-to-end** | a user journey works through the deployed stack | Playwright, Cypress | slow |

The **test pyramid** says "many unit tests, few end-to-end". For web apps the **testing trophy**
(Kent C. Dodds) is often a better guide: a broad base of static checks and a thick middle of
integration tests, because most bugs live where pieces meet.

## The API: test with the real database

Mocking the database tests your mocks. Run the API's integration tests against a **real**
PostgreSQL in a throwaway container (Testcontainers for Java, Node.js, .NET, Go and Python), with
the real migrations applied:

- you catch wrong SQL, missing indexes in plans, constraint violations and transaction bugs;
- each test arranges its own data (or runs in a rolled-back transaction) so tests do not depend
  on each other;
- the HTTP layer is tested through the framework (MockMvc, Supertest, `WebApplicationFactory`):
  status codes, problem details, security rules. You did this in every kata of this track.

## The UI: mock the network, not the components

Test components the way users use them: find elements by role and label, type, click, and assert
on what is visible. Mock the **network** with Mock Service Worker (MSW) handlers that return the
API's real shapes (generated from the contract), and cover every state: loading, empty, error,
validation errors under fields, and permission flags such as `canDelete`.

## Contracts: keep both sides honest

Integration tests on each side can both pass while the two sides disagree. Close the gap with
one of:

- **generated types** from OpenAPI or shared schemas, type-checked in CI (the cheapest);
- **schema validation in tests**: the API's responses are validated against the published schema,
  and the UI's MSW fixtures are too;
- **consumer-driven contracts** (Pact): the UI publishes the interactions it relies on, and the
  API's pipeline verifies them before deploying.

## End-to-end: few, valuable, reliable

Write a handful of end-to-end tests for the **journeys that must never break**: sign up, log in,
checkout. Run them against a deployed environment or a Compose stack. Playwright waits for
elements automatically, traces failures with screenshots and network logs, and runs browsers in
parallel. Rules that keep them trustworthy:

- seed data through the API or the database, not through the UI;
- use stable selectors (roles, labels, `data-testid`), never CSS paths;
- no fixed `sleep`: wait for a state;
- quarantine and fix **flaky** tests at once; a suite that cries wolf gets ignored.

## Fast feedback

On every pull request: static checks, unit, integration, component and contract tests, and the
end-to-end smoke tests. Nightly or before release: the full end-to-end suite, accessibility
scans (axe), visual regression, load tests. Measure coverage to find untested code, not as a
target to game; mutation testing tells you whether tests would catch a bug.

## Summary

- Choose each test's level by what it proves; most value is in integration tests.
- Test the API against a real database; test the UI with a mocked network and real shapes.
- Guard the contract with generated types, schema checks or consumer-driven contracts.
- Keep end-to-end tests few, focused on critical journeys, and free of flakiness.

## Further reading

- Kent C. Dodds, "The Testing Trophy and Testing Classifications": <https://kentcdodds.com/blog/the-testing-trophy-and-testing-classifications>
- Martin Fowler, "The Practical Test Pyramid": <https://martinfowler.com/articles/practical-test-pyramid.html>
- Testcontainers documentation: <https://testcontainers.com/>
- Playwright documentation, "Best Practices": <https://playwright.dev/docs/best-practices>
- Mock Service Worker documentation: <https://mswjs.io/docs/>
- Pact documentation: <https://docs.pact.io/>
