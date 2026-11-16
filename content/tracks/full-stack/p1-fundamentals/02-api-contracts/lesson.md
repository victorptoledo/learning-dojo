# Typed API Contracts Between Client and Server

When the same team writes the page and the API, it is tempting to treat the API as an internal
detail. Then someone renames a field, the compiler of each side is happy, and the page breaks in
production. The API is a **contract**: both sides must agree on it, and something must check
that they do.

## Design for the screens, not for the tables

A good API for a UI:

- uses **resources** with stable identifiers (`/orders/42`, `/orders/42/items`) and plain verbs
  (the Backend Developer track covers REST design in depth);
- returns what a screen needs in **one or two calls**, not one call per row (the N+1 problem
  happens over HTTP too);
- exposes **domain names**, not column names, and never internal fields (password hashes,
  internal flags, foreign keys the client cannot use);
- uses **stable, documented formats**: amounts as integer cents with a currency, timestamps as
  ISO-8601 in UTC (`2026-01-15T08:15:00Z`), ids as strings if they may exceed 2^53.

## One source of truth for types

Three common ways to keep both sides in sync:

| Approach | How | Fits |
| --- | --- | --- |
| **OpenAPI first** | write `openapi.yaml`, generate server stubs and a typed client (`openapi-typescript`, OpenAPI Generator) | different languages, public APIs |
| **Code first + generation** | annotate the server (springdoc, FastAPI, ASP.NET), export OpenAPI in CI, generate the client | a server in Java, C#, Python or Go and a TypeScript front end |
| **Shared schemas** | one TypeScript package with zod (or Valibot) schemas imported by both sides; or tRPC, where the client calls server procedures with inferred types | a TypeScript monorepo |

Whatever you choose, run it in **CI**: regenerate the client and type-check the front end on every
API change, so a rename fails the build, not the users.

## Types are not validation

TypeScript types disappear at runtime. JSON from the network is `unknown` until you check it.

```ts
import { z } from "zod";

export const NewTodo = z.strictObject({
  title: z.string().trim().min(1).max(120),
  due: z.iso.date().optional(),
});
export type NewTodo = z.infer<typeof NewTodo>;   // the type comes from the schema

const parsed = NewTodo.safeParse(await readJson(request));
if (!parsed.success) {
  return problem(422, "/problems/validation", toFieldErrors(parsed.error));
}
```

- The **server** validates everything it receives: it is the trust boundary.
- The **client** can validate what it receives too, at least for critical screens. A clear
  "the server sent an invalid order" error beats `undefined is not a function` three components
  later.
- Derive types from schemas (or schemas from OpenAPI), never maintain both by hand.

## Errors as problem details

Pick one error shape and use it everywhere. [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)
*problem details* is the standard one, with `Content-Type: application/problem+json`:

```json
{
  "type": "/problems/validation",
  "title": "The todo is not valid",
  "status": 422,
  "errors": [{ "path": "title", "message": "Too small: expected string to have >=1 characters" }]
}
```

`type` is a stable identifier the client can switch on; `title` is for humans; extension
members such as `errors` carry the details a form needs to put each message under its field.
Spring's `ProblemDetail`, ASP.NET Core's `ProblemDetails` and libraries in every language produce
this shape.

## Evolving a contract

- **Adding** an optional field or a new endpoint is compatible.
- **Removing or renaming** a field, making one required, or changing its type breaks old clients,
  and old clients exist: cached SPA bundles, mobile apps that update weeks later.
- Roll out breaking changes with **expand and contract**: add the new field next to the old one,
  move every client to it, measure that the old one is no longer used, then remove it. Version
  the API (`/v2`, a media type) only when expand and contract is not enough.
- Be a **tolerant reader**: clients ignore fields they do not know, so servers can add them.

## Summary

- Design payloads for screens, with domain names and stable formats.
- Keep one source of truth (OpenAPI or shared schemas) and check it in CI.
- Validate at runtime at every boundary; derive types from schemas.
- Use problem details for errors, with field errors for forms.
- Change contracts additively; remove things with expand and contract.

## Further reading

- RFC 9457, "Problem Details for HTTP APIs": <https://www.rfc-editor.org/rfc/rfc9457>
- OpenAPI Specification: <https://spec.openapis.org/oas/latest.html>
- Zod documentation: <https://zod.dev>
- tRPC, "Concepts": <https://trpc.io/docs/concepts>
- Martin Fowler, "TolerantReader": <https://martinfowler.com/bliki/TolerantReader.html>
- In the dojo: the API Design track's *Resources and REST*, *Errors as Problem Details*, *Versioning and Evolving an API Without Breaking Clients* and *OpenAPI as a Contract*.
