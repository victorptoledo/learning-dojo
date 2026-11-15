# Testing, Type-Level Tests and Linting

The type checker removes a whole class of bugs, but it says nothing about **behaviour**: a
function that adds instead of subtracting type-checks perfectly. TypeScript projects use three
safety nets together: the type checker, tests, and a linter that catches what neither of them
does. This lesson sets up all three for a modern Node.js project.

## Choosing a test runner

| Runner | Strengths | Use it when |
| --- | --- | --- |
| `node:test` | Built into Node.js: no dependency, runs `.ts` files directly, mocks, snapshots, coverage | Libraries and services on Node; small projects; the dojo's katas |
| Vitest | Fast watch mode, Jest-compatible API, `expectTypeOf`, rich mocking, browser mode | Most application code, front-end projects, teams coming from Jest |
| Jest | The long-time standard | Existing projects; new projects usually pick Vitest |

### node:test

```ts
// tests/cart.test.ts
import assert from "node:assert/strict";
import { describe, test, mock, beforeEach } from "node:test";
import { Cart } from "../src/cart.ts";

describe("Cart", () => {
  let cart: Cart;
  beforeEach(() => {
    cart = new Cart();
  });

  test("totals the items", () => {
    cart.add({ sku: "pen", priceCents: 150 }, 2);
    assert.equal(cart.totalCents(), 300);
  });

  test("notifies listeners", () => {
    const listener = mock.fn();
    cart.onChange(listener);
    cart.add({ sku: "pen", priceCents: 150 }, 1);
    assert.equal(listener.mock.callCount(), 1);
  });

  test("rejects negative quantities", () => {
    assert.throws(() => cart.add({ sku: "pen", priceCents: 150 }, -1), RangeError);
  });
});
```

Run with `node --test` (it finds `*.test.ts` files) or `node --test --watch`. Use
`node:assert/strict`: its `equal` is `===`, and `deepEqual` compares structures strictly. For
time-dependent code, `mock.timers.enable({ apis: ["setTimeout"] })` lets a test move the clock
with `mock.timers.tick(ms)`. Coverage comes from `--experimental-test-coverage` with
thresholds such as `--test-coverage-lines=80`.

Node runs the test files with type stripping, so **tests do not type-check themselves**: run
`tsc` on the test files too (include them in tsconfig), or a typo in a test can make it pass for
the wrong reason.

### Vitest

```ts
import { describe, expect, it, vi } from "vitest";

it("formats prices", () => {
  expect(format(1999)).toBe("19.99");
  expect(() => format(-1)).toThrowError(/negative/);
});
```

Vitest uses esbuild (or Oxc) to strip types, so it also needs `tsc` alongside, or its
`--typecheck` mode.

## What to test

- **Pure functions and domain rules** with plain unit tests: fast and stable.
- **Boundaries** (parsers, schemas, HTTP handlers) with valid and invalid input; the invalid
  cases are where the bugs live.
- **Integration** with real dependencies where it matters (a real database in a container),
  fewer and slower.
- Avoid testing what the type checker already guarantees ("the function returns a string").
  Test behaviour instead.

Design for testability by **injecting** what is hard to control: pass a clock, a random source,
a `fetch` or a repository as parameters instead of importing globals. The `wait` option of the
retry kata is an example.

## Type-level tests

When the **types** are the product (a library, a generic helper, a typed API client), test them:

```ts
import { expectTypeOf } from "vitest";            // or the expect-type package
expectTypeOf(pluck(users, "id")).toEqualTypeOf<number[]>();
expectTypeOf(parse).parameter(0).toBeString();

// without a library:
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends <T>() => T extends B ? 1 : 2 ? true : false;
type Expect<T extends true> = T;
type _ = Expect<Equal<ReturnType<typeof parseUserId>, UserId | null>>;

// @ts-expect-error: only keys of User are allowed
pluck(users, "email");
```

`@ts-expect-error` fails when the next line has **no** error, so it is a test that something is
rejected. Do not confuse it with `@ts-ignore`, which silences errors forever and should almost
never appear in code.

These tests run as part of `tsc`. The dojo runs exactly this for the katas' `*.types.ts` files.

## Linting

`tsc` checks types; a linter checks **patterns**. ESLint with `typescript-eslint` is the
standard:

```js
// eslint.config.js (flat config)
import js from "@eslint/js";
import { defineConfig } from "eslint/config";
import tseslint from "typescript-eslint";

export default defineConfig(
  js.configs.recommended,
  tseslint.configs.strictTypeChecked,
  { languageOptions: { parserOptions: { projectService: true } } },
);
```

The **type-checked** rules are the valuable ones, because they use the compiler's knowledge:

| Rule | Catches |
| --- | --- |
| `no-floating-promises` | A promise that is neither awaited nor handled |
| `no-misused-promises` | An async function passed where a void callback is expected (`forEach`, event handlers) |
| `no-unsafe-*` (`assignment`, `member-access`, `call`, `return`) | `any` spreading through the code |
| `switch-exhaustiveness-check` | A `switch` that misses a union member |
| `no-unnecessary-condition` | A check the types prove always true or always false |
| `restrict-template-expressions` | `${object}` printing `[object Object]` |

With TypeScript 7, `typescript-eslint` still needs the 6.0 compiler API, so projects install
`@typescript/typescript6` alongside (next phase). Biome and Oxlint are much faster Rust-based
linters; they cover many rules but only part of the type-aware ones.

**Formatting** is a separate job: let Prettier or Biome format the code and turn off style
rules in the linter, so reviews never discuss commas.

## Putting it together in CI

```json
{
  "scripts": {
    "typecheck": "tsc",
    "lint": "eslint .",
    "test": "node --test",
    "check": "npm run typecheck && npm run lint && npm test"
  }
}
```

Run `check` on every pull request, and make each step fail the build. A pre-commit hook can run
the fast parts (format, lint on changed files) locally.

## Summary

- Types do not test behaviour: write tests, with `node:test` or Vitest, and type-check them too.
- Inject time, randomness and I/O to make code testable.
- Test types with `Expect<Equal<...>>`, `expectTypeOf` and `@ts-expect-error`; avoid `@ts-ignore`.
- Lint with typescript-eslint's type-checked rules (`no-floating-promises` first); format
  separately; run type check, lint and tests in CI.

## Further reading

- Node.js, "Test runner": <https://nodejs.org/api/test.html>
- Vitest, "Testing Types": <https://vitest.dev/guide/testing-types>
- typescript-eslint, "Linting with Type Information": <https://typescript-eslint.io/getting-started/typed-linting/>
- typescript-eslint, `no-floating-promises`: <https://typescript-eslint.io/rules/no-floating-promises/>
- TypeScript 3.9 release notes, `// @ts-expect-error` comments: <https://www.typescriptlang.org/docs/handbook/release-notes/typescript-3-9.html>
- roadmap.sh, TypeScript topic "Linting": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/typescript/content/linting@8PcAPOrKisKRYPWamz4nV.md>
