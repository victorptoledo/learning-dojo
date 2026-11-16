---
from: csharp
status: draft
concept: "Exceptions, options and tests"
from_way: "Exceptions with InnerException, IOptions<T> validated with ValidateOnStart, and xUnit or NUnit tests with Moq and WebApplicationFactory."
native_way: "Errors with cause, a validated config object built at startup, and node:test with built-in mocks and a real server on port 0."
sensei_tip: "ValidateOnStart is exactly the fail-fast idea: parse process.env before listening. And WebApplicationFactory's in-memory server becomes a real listen(0)."
---

| C# | Node.js |
| --- | --- |
| `new Exception("x", inner)` | `new Error("x", { cause })` |
| `services.AddOptions<T>().ValidateOnStart()` | Validate `process.env` before `listen` |
| `[Fact]` | `test("...", () => {})` |
| `Assert.ThrowsAsync<T>` | `assert.rejects(p, T)` |
| Moq `Setup` / `Verify` | `t.mock.fn()`, `mock.callCount()` |
| `FakeTimeProvider` | `t.mock.timers` |
