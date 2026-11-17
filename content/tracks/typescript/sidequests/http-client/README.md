# Sidequest: Typed HTTP client

`fetch` is easy to call and easy to call wrong: it throws on a dead network but not on a 500,
it gives you `any` from `.json()`, and nothing stops `/users/:id` from going out with no id.
Build `client.ts`, a small HTTP client on top of the global `fetch` where every failure is a
value of a discriminated union, and the compiler knows each endpoint of your API.

```ts
import { createApiClient, defineApi } from "./client.ts";

const api = defineApi({
  getUser: { method: "GET", path: "/users/:id", decode: parseUser },
  createUser: { method: "POST", path: "/users", encode: (user: NewUser) => user, decode: parseUser },
});
const users = createApiClient(api, { baseUrl: "https://api.example.com", retries: 2, timeoutMs: 2000 });

const result = await users.getUser({ params: { id: "42" } });
if (result.ok) console.log(result.value.name);
else if (result.error.kind === "status" && result.error.status === 404) console.log("no such user");
```

The contract tests start a real HTTP server on `127.0.0.1` inside the container and call the
functions each stage asks you to export; type-level tests check what compiles and what must
not. The exported names are fixed, everything behind them is yours. No `any` and no
libraries: `fetch`, `AbortSignal` and `URL` are all you need. `tests/` is yours too.
