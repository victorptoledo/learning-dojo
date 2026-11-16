---
from: go
status: reviewed
concept: "Table tests plus real dependencies"
from_way: "Interfaces mocked by hand for every dependency, and no tests across the HTTP boundary."
native_way: "Table-driven unit tests, httptest for handlers, testcontainers-go for PostgreSQL, validation against the OpenAPI spec, and Playwright for the UI journeys."
sensei_tip: "httptest.NewServer gives you a real HTTP server on a free port in one line, as the katas of this track do with node:http."
---

| Go | Across the stack |
| --- | --- |
| hand-written mocks | testcontainers-go |
| `httptest.NewRecorder` | handler tests |
| kin-openapi validation | contract checks |
| playwright-go | end-to-end journeys |
