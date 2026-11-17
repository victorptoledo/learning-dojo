A second consumer proves the core is really shared, and tests get helpers that production
code must never ship with.

**A test kit subpath.** `@tally/core` gains a second entry, `"./testing": "./src/testing.ts"`
in its `exports` (exactly these two keys, `.` and `./testing`). `@tally/core/testing`
exports `moneyOf(text: string): Money`, which parses like `parseMoney` but throws an `Error`
whose message is the parse error, handy in tests. `moneyOf` is **not** exported by
`@tally/core` itself, and no file under `packages/*/src` outside core imports
`@tally/core/testing`.

**The API package.** Create `packages/api/`, the package `@tally/api`, like the CLI: its
`package.json` (`exports` `.` → `./src/index.ts`, `dependencies` `@tally/core` `^<version>`),
its `tsconfig.json` project referencing core, a place in the root solution, and imports of
`@tally/core` only (not of the CLI). It exports the types
`ApiRequest = { method: string; path: string; body?: unknown }`,
`ApiResponse = { status: number; body: unknown }` and `createHandler(): (request: ApiRequest) => ApiResponse`.
Each handler keeps its own expenses in memory; ids are `"1"`, `"2"`... in creation order,
never reused.

| Request | Answer |
|---|---|
| `POST /expenses` with `{ date, amount, description }` | `201`, the expense `{ id, date, amount, description }`, `amount` written by `formatMoney` |
| `GET /expenses` | `200`, every expense in creation order |
| `GET /expenses/<id>` | `200` and the expense, or `404` `{ error: "no expense <id>" }` |
| `DELETE /expenses/<id>` | `204` with body `null`, or the same `404` |
| `GET /totals` | `200`, `{ "<currency>": "<total>" }` for the currencies that appear, in the order `BRL`, `USD`, `EUR` (`{}` when there are none) |
| another path | `404` `{ error: "not found" }` |
| a path above with another method | `405` `{ error: "method not allowed" }` |

A `POST` that is not valid answers `400` `{ error }`, checking in this order: the body is
not a plain object (`body must be an object`); `date` is not a `YYYY-MM-DD` string
(`invalid date: <date>`, the value as `String(date)` writes it); `amount` is not a string (`amount must be text`) or `parseMoney`
rejects it (its error); `description` is not a string with something besides spaces
(`missing description`). The description is stored trimmed.
