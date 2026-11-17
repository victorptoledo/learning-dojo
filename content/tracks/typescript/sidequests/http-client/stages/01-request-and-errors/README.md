One request, and no exceptions. Export from `client.ts`:

```ts
export type Method = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export type HttpError =
  | { kind: "network"; message: string }               // fetch itself failed
  | { kind: "status"; status: number; body: string }   // the server answered, but not 2xx
  | { kind: "decode"; message: string };               // the body is not JSON, or decode threw

export type Result<T, E = HttpError> = { ok: true; value: T } | { ok: false; error: E };

export type Query = Record<string, string | number | boolean | undefined>;

export type RequestOptions<T> = {
  method: Method;
  path: string;
  query?: Query;
  body?: unknown;
  decode: (json: unknown) => T;
};

export type ClientOptions = { baseUrl: string };

export function createClient(options: ClientOptions): {
  request<T>(options: RequestOptions<T>): Promise<Result<T>>;
};
```

How a request goes out:

- The URL is `baseUrl` followed by `path`; a `/` at the end of `baseUrl` is not doubled
  (`http://h/api/` + `/users` is `http://h/api/users`).
- `query` becomes the query string in the order of its keys, encoded like `URLSearchParams`
  does; `undefined` values are left out, and an empty query adds no `?`.
- Every request sends `accept: application/json`. With a `body`, it is sent as
  `JSON.stringify(body)` with `content-type: application/json`; without one, no body is sent.

How the answer comes back:

- `request` **never rejects**: every failure is `{ ok: false, error }`.
- `fetch` throwing (connection refused, bad URL) is a `network` error with the error's message.
- A status outside 200–299 is a `status` error with the status and the body as text.
- A 2xx answer with an empty body (a `204`, for instance) calls `decode(undefined)`. Any other
  body is parsed as JSON: invalid JSON is a `decode` error, and so is `decode` throwing (its
  message is the error's message). Otherwise the value is what `decode` returned.
