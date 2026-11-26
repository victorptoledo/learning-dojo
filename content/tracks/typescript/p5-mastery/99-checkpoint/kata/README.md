# Checkpoint 5 🥋: Feature Flags SDK

The final exam: finish **`@dojo/flags`**, a small feature-flag library that other teams
install. It uses most of the track: literal types kept with a `const` type parameter,
conditional and mapped types, a zod schema built at runtime, async refreshes with a timeout
and a race, typed listeners, a generator, a package boundary and one soundness hole to close.

```ts
const flags = defineFlags({
  darkMode: { kind: "boolean", default: false },
  maxUploads: { kind: "number", default: 3, min: 1, max: 10 },
  checkout: { kind: "variant", variants: ["classic", "one-page", "wizard"], default: "classic" },
});
const client = createClient(flags);
client.get("checkout");                      // "classic" | "one-page" | "wizard"
await client.refresh((signal) => fetchOverrides(signal));
```

The tests import the package **by its name**, `@dojo/flags`. A package can import itself by
name when its `package.json` has `exports`; Node.js resolves that to the package's own files
(not through `node_modules`), so it runs the `.ts` sources directly.

## package.json

`exports` is the public API. The starter also exports `"./*"`, which lets anyone import any
internal file. Close it: only `.` (and `./package.json`) stay reachable, at compile time and at
runtime.

## src/definitions.ts

- **`defineFlags(definitions)`** returns its argument, keeping every literal type (a `const`
  type parameter). A variant flag whose `default` is not one of its `variants` is a **compile
  error**. At runtime (for JavaScript callers) it throws a `FlagsError` for a variant default
  that is not a variant, a number default outside `[min, max]` (or `min > max`), and a flag name
  that does not match `/^[a-z][A-Za-z0-9]*$/`.
- **`FlagValue<F>`**: `boolean`, `number`, or the **union of the variants**.
- **`FlagEntry<D>`**: a union with one `readonly [key, value]` tuple per flag, so that checking
  the key narrows the value.

## src/overrides.ts

`parseOverrides(definitions, input: unknown)` builds a **zod** schema from the definitions:
every flag optional, each value checked by its kind (numbers in range, variants from the list),
and **no other keys**. It returns `{ ok: true, overrides }` or `{ ok: false, issues }` with
`"path: message"` issues: `"(root): ..."` for input that is not an object, and
`"<key>: unknown flag"` for each key that is not a flag.

## src/client.ts

`createClient(definitions, { overrides? })` validates the initial overrides (a `FlagsError`
with the `issues` when they are invalid) and returns a `FlagClient`:

| Member | Does |
| --- | --- |
| `get(key)` | The override, or the default. Typed by key; an unknown key at runtime (including inherited names such as `"toString"`) throws a `FlagsError` |
| `snapshot()` | Every value in a **frozen** new object |
| `entries()` | A generator of `[key, value]` in the order of the definitions |
| `subscribe(key, listener)` | `listener(value, previous)` after a refresh changes that flag; returns a function that removes it |
| `refresh(load, { timeoutMs = 1000 })` | See below |

**`Listener<V>`** is written as a method in the starter, which TypeScript checks
**bivariantly**: a listener that only accepts `"wizard"` is accepted for a flag with three
variants. Make it a sound function type.

`refresh` calls `load(signal)` and applies the payload it returns:

- The payload **replaces** all overrides: flags it does not mention go back to their default.
  It is applied **all or nothing**: an invalid payload throws a `FlagsError` with the `issues`
  and changes nothing.
- A `load` that rejects throws a `FlagsError` whose `cause` is the original error. After
  `timeoutMs` the signal given to `load` is aborted and the refresh throws a `FlagsError` whose
  `cause` is the `TimeoutError`, **even if `load` ignores the signal**. Neither changes the state.
- If another refresh **started** after this one while it was loading, this one is stale: it
  returns `{ changed: [], stale: true }` and changes nothing.
- Otherwise it returns `{ changed, stale: false }`, `changed` in definition order, and then
  calls the listeners of the changed flags, once the whole new state is in place. A listener
  that throws does not stop the others; their errors are thrown together as an
  `AggregateError` after all of them ran (the new state stays).

`src/errors.ts` and `src/index.ts` are done.

Run the tests with `npm test`. There are 25 tests: 1 in `tests/visible.test.ts` and 24 hidden
ones, 7 of them type-level.
