---
from: javascript
status: reviewed
concept: "Testing like a user, with a mocked network"
from_way: "Snapshot tests of component trees and jest.mock of the API module."
native_way: "Testing Library queries by role and label, MSW handlers with contract-shaped data, Supertest or the framework's injector for the API, and Playwright for journeys."
sensei_tip: "Generate MSW handlers' types from the OpenAPI spec so mocks cannot drift from the real API."
---

| JavaScript | Across the stack |
| --- | --- |
| snapshot tests | `getByRole`, `getByLabelText` and user events |
| `jest.mock("./api")` | MSW at the network boundary |
| Supertest | API tests |
| Playwright | end-to-end with traces |
