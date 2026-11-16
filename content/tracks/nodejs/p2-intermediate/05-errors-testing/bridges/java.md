---
from: java
status: draft
concept: "Exceptions, configuration and tests"
from_way: "Checked and unchecked exceptions, exception chaining with a cause, @ConfigurationProperties with validation at startup, and JUnit with Mockito."
native_way: "One Error type with name, code and cause, configuration validated by hand or with zod at startup, and node:test with node:assert and built-in mocks."
sensei_tip: "There are no checked exceptions, so nothing reminds you what can fail. The operational vs programmer distinction does the job of checked vs unchecked: handle the first, let the second crash."
---

| Java | Node.js |
| --- | --- |
| `new RuntimeException("x", cause)` | `new Error("x", { cause })` |
| `@Validated @ConfigurationProperties` | A schema check of `process.env` at startup |
| JUnit `@Test`, `@BeforeEach` | `test()`, `beforeEach()` |
| `assertThrows` | `assert.throws` / `assert.rejects` |
| Mockito `mock()`, `verify()` | `t.mock.fn()`, `mock.calls` |
| `@SpringBootTest(webEnvironment = RANDOM_PORT)` | `server.listen(0)` + `fetch` |
