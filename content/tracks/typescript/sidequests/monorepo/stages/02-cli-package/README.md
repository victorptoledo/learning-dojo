The first consumer. Create `packages/cli/`, the package `@tally/cli`:

- `package.json`: `"name": "@tally/cli"`, a `"version"`, `"type": "module"`,
  `"exports": { ".": "./src/index.ts" }` and `"dependencies": { "@tally/core": "^<core's version>" }`
  (`"^1.0.0"` while core is `1.0.0`).
- **The boundary:** the CLI uses core only as `import ... from "@tally/core"`. No relative
  path out of `packages/cli/` (`../../core/src/money.ts`) and no other `@tally/` package;
  the tests read every import under `packages/cli/src/`.
- It exports `type Io = { readFile(path: string): string; out(line: string): void; err(line: string): void }`
  and `run(argv: readonly string[], io: Io): number`, which returns the exit code. A real
  entry point is a two-line `src/main.ts` that calls `run(process.argv.slice(2), ...)`; the
  tests call `run` with a fake `Io`.

`run(["sum", file])` adds up a ledger file, one expense per line:

```text
# November
2026-11-01  12.50 BRL  coffee with Ana
2026-11-03  4.99 USD   app subscription
2026-11-10  30 BRL     lunch
```

- A line is a date `YYYY-MM-DD`, an amount and a currency (as `parseMoney` reads them) and
  a description of at least one word, separated by whitespace. Blank lines and lines
  starting with `#` are skipped.
- It prints one line per currency that appears, the total as `formatMoney` writes it, in
  the order `BRL`, `USD`, `EUR`: `42.50 BRL` then `4.99 USD`. A ledger with no expenses
  prints `no expenses`. The exit code is 0.
- `--month YYYY-MM` after the file keeps only the expenses of that month.

Errors print one line on `err`, nothing on `out`:

| When | Code | `err` |
|---|---|---|
| no command, another command, a missing file, an unknown option, `--month` without a valid `YYYY-MM` | 2 | `usage: tally sum <file> [--month YYYY-MM]` |
| `readFile` throws | 1 | `error: cannot read <file>` |
| a line is wrong (the first one only) | 1 | `line <n>: <reason>` |

The line number counts every line from 1, skipped ones included, and the reason is
`invalid date: <token>`, the error of `parseMoney` for the amount and currency, or
`missing description`.
