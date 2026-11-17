Describe the API once and let the compiler check every call. Export from `client.ts`:

- `type PathParams<P extends string>`: the parameters of a path template, each a `string`.
  A parameter is a segment written `:name` (letters, digits and `_`):
  `PathParams<"/users/:id/posts/:postId">` has exactly the keys `id` and `postId`, and
  `PathParams<"/users">` has none.
- `type Endpoint`: `{ method: Method; path: string; encode?: (input: never) => unknown;
  decode: (json: unknown) => unknown }`. `encode` turns the call's `body` into what is sent
  as JSON; an endpoint without `encode` sends no body.
- `type Api = Record<string, Endpoint>`.
- `defineApi(api)`: returns `api` unchanged, typed with its literal paths and methods, so
  nobody has to write `as const` (a `const` type parameter does it).
- `createApiClient(api, options: ClientOptions)`: one method per endpoint, with the same
  name, built on `createClient(options).request`. Each method takes one argument:

| Field | When | Type |
|---|---|---|
| `params` | the path has parameters (then it is required) | `PathParams<path>` |
| `body` | the endpoint has `encode` (then it is required) | the type of `encode`'s parameter |
| `query` | always optional | `Query` |

  When nothing is required, the argument itself is optional: `users.listUsers()`. Passing
  `params` to a path without parameters, or `body` to an endpoint without `encode`, must not
  compile, and neither must a missing or misspelled parameter. The method returns
  `Promise<Result<T>>`, where `T` is what the endpoint's `decode` returns.

At run time, each `:name` of the path is replaced by `encodeURIComponent(params[name])`
(`"a b/c"` goes out as `a%20b%2Fc`), and the body sent is `encode(body)`.
