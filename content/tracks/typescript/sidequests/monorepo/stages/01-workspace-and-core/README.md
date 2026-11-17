The workspace is set up: the root `package.json` declares `"workspaces": ["packages/*"]`,
and `tsconfig.base.json` holds the settings every package will share. Create the first
package in `packages/core/`:

- `packages/core/package.json`: `"name": "@tally/core"`, a `"version"`, `"type": "module"`
  and `"exports": { ".": "./src/index.ts" }`. The `exports` map is the package's public
  surface: no wildcards, so `import "@tally/core/src/money.ts"` from another package must
  not resolve. Put the code wherever you like under `src/`.
- What `@tally/core` exports:

| Export | What it is |
|---|---|
| `type Currency` | `"BRL" \| "USD" \| "EUR"` |
| `type Money` | `{ readonly cents: number; readonly currency: Currency }` plus a **brand**: an object literal must not be assignable to it, so `money()` and `parseMoney()` are the only ways to get one |
| `money(cents, currency): Money` | throws a `RangeError` when `cents` is not a safe integer (`1.5`, `NaN`, `2 ** 60`) |
| `isCurrency(value: unknown): value is Currency` | a type guard |
| `type Parsed` | `{ ok: true; value: Money } \| { ok: false; error: string }` |
| `parseMoney(text: string): Parsed` | see below |
| `formatMoney(value: Money): string` | `12.50 BRL`, `-0.05 USD`, `1234.00 EUR`: two decimals, a minus sign for negatives, no thousands separator |
| `add(a, b): Money` | throws a `TypeError` with message `cannot add BRL and USD` when the currencies differ |
| `sum(values: readonly Money[], currency): Money` | `money(0, currency)` for an empty list, the same `TypeError` as `add` on a mix |

`parseMoney` reads an amount and a currency separated by whitespace, ignoring whitespace
around them. The amount is an optional `-`, digits, and optionally `.` with one or two
digits: `12`, `12.5` (1250 cents), `-0.05`. Its cents are computed exactly, never through
floating point (`0.1 + 0.2` is not a good idea with money). The errors, checked in order:

| When | `error` |
|---|---|
| it is not exactly two parts | `invalid money: <text>` |
| the currency is not a `Currency` (case matters) | `unknown currency: <currency>` |
| the amount does not match, or is not a safe number of cents | `invalid amount: <amount>` |
