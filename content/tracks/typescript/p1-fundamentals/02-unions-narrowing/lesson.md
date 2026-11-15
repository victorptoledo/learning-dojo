# Unions, Literal Types and Narrowing

Real data comes in several forms: a value is a string *or* a number, a response is a success
*or* an error, a field is set *or* missing. TypeScript models this with **union types**, and
the compiler follows your `if` statements to know which form you hold at each line. This is
called **narrowing**, and it is the heart of everyday TypeScript.

## Union types

```ts
type Id = string | number;

function format(id: Id): string {
  return id.toUpperCase();   // error: Property 'toUpperCase' does not exist on type 'number'
}
```

On a union you may only use what **every** member has. To use more, narrow first:

```ts
function format(id: Id): string {
  if (typeof id === "string") {
    return id.toUpperCase();   // id: string here
  }
  return id.toFixed(0);        // id: number here
}
```

## Literal types

A literal type is a single value used as a type. Unions of literals replace most enums:

```ts
type Direction = "north" | "south" | "east" | "west";
type Dice = 1 | 2 | 3 | 4 | 5 | 6;

function move(direction: Direction) {}
move("north");
move("up");    // error: Argument of type '"up"' is not assignable to parameter of type 'Direction'
```

Inference **widens** literals in mutable places: `let d = "north"` is a `string`, and an
object literal `{ dir: "north" }` has `dir: string`. Use a `const`, an annotation, or
`as const` (phase 2) to keep the literal.

## The narrowing toolbox

| Check | Narrows by | Example |
| --- | --- | --- |
| `typeof x === "string"` | Primitive kind | `"string"`, `"number"`, `"bigint"`, `"boolean"`, `"symbol"`, `"undefined"`, `"object"`, `"function"` |
| Truthiness `if (x)` | Removes `null`, `undefined` (and `""`, `0`, `false`!) | `if (user) user.name` |
| `x === null`, `x !== undefined` | Equality | `if (value != null)` removes both null and undefined |
| `"key" in x` | Property presence | `if ("swim" in animal)` |
| `x instanceof Date` | Class (prototype chain) | Errors, dates, your classes |
| `Array.isArray(x)` | Arrays | Before indexing unknown data |
| A discriminant `x.kind === "circle"` | Tagged unions | See below |

Narrowing flows through `return`, `throw`, `else`, `switch`, `&&`, `||`, `??` and early
`continue`. TypeScript does this **control flow analysis** on every function.

Beware of truthiness on numbers and strings: `if (count)` is false for `0`, a perfectly valid
count. Compare with `!== undefined` when zero or the empty string are valid values.

## Discriminated unions

The most useful pattern in TypeScript: each member of a union has a common property with a
different **literal** value, the *discriminant* (also *tag*).

```ts
type Loading = { state: "loading" };
type Loaded = { state: "loaded"; data: string[] };
type Failed = { state: "failed"; error: Error };
type Request = Loading | Loaded | Failed;

function render(request: Request): string {
  switch (request.state) {
    case "loading":
      return "…";
    case "loaded":
      return request.data.join(", ");   // request: Loaded
    case "failed":
      return request.error.message;     // request: Failed
  }
}
```

Compare this with one type full of optional fields (`{ loading?: boolean; data?: string[];
error?: Error }`): that version allows impossible states (`data` *and* `error`), and every
reader has to guess which fields are set. **Make impossible states impossible to represent.**

## never and exhaustiveness

`never` is the type with **no values**: the empty union. After every case has been handled,
the remaining type is `never`. Use it to make the compiler check that a `switch` handles every
member:

```ts
function assertNever(value: never): never {
  throw new Error(`unexpected: ${JSON.stringify(value)}`);
}

function icon(request: Request): string {
  switch (request.state) {
    case "loading": return "⏳";
    case "loaded":  return "✅";
    case "failed":  return "❌";
    default:        return assertNever(request);
  }
}
```

Add `{ state: "cancelled" }` to `Request` and `icon` stops compiling: "Argument of type
'Cancelled' is not assignable to parameter of type 'never'". That turns a forgotten case into a
compile error everywhere it matters.

`never` also appears as the return type of functions that never return (they always throw, or
loop forever), and in conditional types that filter members out (phase 2).

## unknown vs any

Both accept every value. The difference is what you may do with it afterwards:

| | `any` | `unknown` |
| --- | --- | --- |
| Assign anything to it | Yes | Yes |
| Use it (call, read properties, pass on as a `string`) | Yes, unchecked | No, narrow first |
| Typical source | Old code, bad typings | `JSON.parse` results you type yourself, `catch (error)`, external input |

```ts
function lengthOf(value: unknown): number {
  if (typeof value === "string" || Array.isArray(value)) {
    return value.length;
  }
  throw new TypeError("expected a string or an array");
}
```

Treat `unknown` as "I have not checked this yet". `any` means "never check this", and it
spreads: `any.foo` is `any`, and a function returning it infects every caller.

## Type predicates and assertion functions

When a check is too complex to write inline, put it in a function and tell the compiler what
it proves:

```ts
function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function assertString(value: unknown, name: string): asserts value is string {
  if (typeof value !== "string") throw new TypeError(`${name} must be a string`);
}
```

`value is T` narrows in the caller's `if`; `asserts value is T` narrows everything after the
call. The compiler **trusts** the predicate, so a wrong predicate is a lie it cannot catch:
test them. Since TypeScript 5.5, simple arrow functions such as `x => x !== undefined` infer
their predicate by themselves, so `values.filter((x) => x !== undefined)` has type `T[]`.

## Optional chaining and nullish coalescing

```ts
const city = user.address?.city ?? "unknown";  // ?. stops at null/undefined, ?? falls back on them only
const port = config.port ?? 8080;               // 0 stays 0; with || it would become 8080
```

## Summary

- A union allows several forms; use only shared members until you narrow.
- Narrow with `typeof`, equality, `in`, `instanceof`, `Array.isArray`, truthiness (carefully)
  and discriminants; the compiler follows control flow.
- Model states as discriminated unions, and close every `switch` with `assertNever`.
- `never` is the empty type. `unknown` is the safe "any value": narrow before use.
- Type predicates (`value is T`) and assertion functions extend narrowing; test them.

## Further reading

- TypeScript Handbook, "Narrowing": <https://www.typescriptlang.org/docs/handbook/2/narrowing.html>
- TypeScript Handbook, "Everyday Types", union types and literal types: <https://www.typescriptlang.org/docs/handbook/2/everyday-types.html#union-types>
- TypeScript 5.5 release notes, inferred type predicates: <https://www.typescriptlang.org/docs/handbook/release-notes/typescript-5-5.html>
- MDN, `typeof`: <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/typeof>
- roadmap.sh, TypeScript topic "Type Guards / Narrowing": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/typescript/content/type-guards--narrowing@cFZsxpYHDxm7IWwergb3r.md>
