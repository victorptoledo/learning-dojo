---
from: javascript
status: draft
concept: "The same async model, with types for results and narrowing for errors"
from_way: "Promises and async/await, catch receives whatever was thrown, and nothing says what a promise resolves to."
native_way: "Promise<T> types the result, catch receives unknown that must be narrowed, and Result unions make expected failures visible in signatures."
sensei_tip: "Nothing about the runtime changes. What changes is that you must prove what you caught before using it, and the compiler finally tells callers what an async function returns."
---

| JavaScript | TypeScript |
| --- | --- |
| `async function load()` | `async function load(): Promise<User>` |
| `catch (e) { e.message }` | `catch (e) { if (e instanceof Error) e.message }` |
| `return { error: "x" }` sometimes | `Result<T, E>` union, checked by the compiler |
| `class HttpError extends Error` | The same, with typed fields and `override name` |
