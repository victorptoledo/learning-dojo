# Validate at the Boundary

TypeScript's types disappear when the program runs, so data that comes from outside (a JSON
file, environment variables, an HTTP body) must be **checked at runtime** before the rest of
the code can trust its type. In `config.ts`, write a zod schema once and let TypeScript infer
the types from it. The dojo image has zod 4 installed; locally, `npm install` adds it.

## 1. The schema

Replace `z.object({})` in `ConfigSchema` with:

| Path | Rule | Default |
| --- | --- | --- |
| `server.host` | a non-empty string | `"0.0.0.0"` |
| `server.port` | an integer from 1 to 65535 | required |
| `database.url` | a URL (`z.url()`) | required |
| `database.poolSize` | a positive integer | `10` |
| `logLevel` | one of `"debug"`, `"info"`, `"warn"`, `"error"` | `"info"` |
| `features` | an array of strings | `[]` |

`Config` (`z.infer`) and `ConfigInput` (`z.input`) then follow by themselves: in the input a
field with a default is optional, in the output it is always there. Unknown keys are dropped.

Make `UserIdSchema` accept `usr_` followed by exactly 8 lower-case letters or digits, and
**brand** it (`.brand<"UserId">()`): a `UserId` is then a string that only `parseUserId` can
produce, and a plain string literal no longer type-checks as one.

## 2. The functions

| Function | Does |
| --- | --- |
| `parseConfig(input: unknown)` | `{ ok: true, value }` or `{ ok: false, error }` with **every** problem as `"path: message"`, the path joined with dots (`"server.port: Too big: ..."`, `"features.1: ..."`) |
| `parseConfigJson(text)` | `JSON.parse`, then `parseConfig`; `{ ok: false, error: ["invalid JSON"] }` when the text is not JSON |
| `configFromEnv(env)` | Builds the input from variables, then `parseConfig` |
| `parseUserId(text)` | The branded id, or `null` |

`configFromEnv` reads `APP_HOST`, `APP_PORT`, `DATABASE_URL`, `DB_POOL_SIZE`, `LOG_LEVEL` and
`FEATURES` (comma-separated, each trimmed, empty ones dropped). A missing variable must reach
the schema as `undefined`, so its default applies. A number variable that is not a number
(`"eighty"`, or an empty string) must be **rejected**, not turned into `0`.

Run the tests with `npm test` after `npm install`. There are 18 tests: 1 in
`tests/visible.test.ts` and 17 hidden ones, 3 of them type-level.
