---
from: python
status: reviewed
concept: "pytest from the database to the browser"
from_way: "unittest.mock everywhere and SQLite in tests while production runs PostgreSQL."
native_way: "pytest with testcontainers or a real PostgreSQL, the framework's test client for HTTP, schemathesis for API contracts, and pytest-playwright for journeys."
sensei_tip: "schemathesis generates requests from your OpenAPI schema and finds responses that break it: a contract test for free."
---

| Python | Across the stack |
| --- | --- |
| SQLite in tests | testcontainers PostgreSQL |
| `TestClient(app)` | HTTP-level tests |
| schemathesis | contract and fuzz tests from OpenAPI |
| pytest-playwright | end-to-end journeys |
