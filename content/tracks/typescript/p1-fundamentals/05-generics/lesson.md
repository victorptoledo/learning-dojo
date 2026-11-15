# Generics, Constraints, keyof and Indexed Access

A function like `first(items)` works for any kind of array. Typing it as `(items: unknown[])
=> unknown` throws the information away: the caller gets `unknown` back and has to cast. A
**generic** keeps it: the type becomes a parameter, filled in by inference at each call.

## Type parameters

```ts
function first<T>(items: readonly T[]): T | undefined {
  return items[0];
}

const n = first([1, 2, 3]);        // number | undefined
const s = first(["a", "b"]);       // string | undefined
const u = first<boolean>([]);      // explicit type argument, rarely needed
```

`T` is decided at each call from the arguments. Generic types work the same way:

```ts
type Box<T> = { value: T };
interface Page<Item> {
  items: Item[];
  next?: string;
}
type Result<T, E = Error> = { ok: true; value: T } | { ok: false; error: E }; // default type argument
```

Built-in generics you use every day: `Array<T>`, `Promise<T>`, `Map<K, V>`, `Set<T>`,
`Record<K, V>`, `ReadonlyArray<T>`.

A useful test for a type parameter: **does it relate two things?** In `first`, `T` links the
input array to the output. A parameter used only once (`function log<T>(x: T): void`) adds
nothing; write `(x: unknown)` instead.

## Constraints

`extends` limits what a type parameter may be, so the body can use those members:

```ts
function longest<T extends { length: number }>(a: T, b: T): T {
  return a.length >= b.length ? a : b;
}

longest("tree", "dojo");    // string
longest([1, 2], [1]);       // number[]
longest(10, 20);            // error: number has no length
```

The return type is still `T`, the caller's precise type, not `{ length: number }`.

## keyof and indexed access

`keyof T` is the union of the keys of `T`; `T[K]` is the type of property `K`:

```ts
type User = { id: number; name: string; admin: boolean };
type UserKey = keyof User;         // "id" | "name" | "admin"
type Name = User["name"];          // string
type IdOrName = User["id" | "name"]; // number | string

function get<T, K extends keyof T>(object: T, key: K): T[K] {
  return object[key];
}

const user: User = { id: 1, name: "Ada", admin: true };
get(user, "name");   // string
get(user, "email");  // error: '"email"' is not assignable to '"id" | "name" | "admin"'
```

This pair (`K extends keyof T`, returning `T[K]`) is the most common generic pattern after
arrays: it powers `pluck`, `sortBy`, form libraries and ORMs.

`typeof` in a type position takes the type of a value, which avoids writing a type twice:

```ts
const defaults = { retries: 3, verbose: false };
type Options = typeof defaults;        // { retries: number; verbose: boolean }
type Keys = keyof typeof defaults;     // "retries" | "verbose"
const levels = ["debug", "info"] as const;
type Level = (typeof levels)[number];  // "debug" | "info"
```

## How inference picks T

TypeScript infers type arguments from the arguments, left to right, and **widens** literals
unless something asks it not to:

```ts
function identity<T>(value: T): T { return value; }
identity("north");          // "north" (a literal is kept when T is returned directly)
function wrap<T>(value: T): T[] { return [value]; }
let list = wrap("north");   // string[]
```

When you want literal types inferred, use a **`const` type parameter** (TypeScript 5.0):

```ts
function routes<const T extends readonly string[]>(paths: T): T { return paths; }
const r = routes(["/home", "/about"]);   // readonly ["/home", "/about"]
```

And when one argument should *not* be used for inference, wrap it in `NoInfer<T>` (5.4):

```ts
function choose<T extends string>(options: T[], fallback: NoInfer<T>): T { return fallback; }
choose(["red", "green"], "blue");   // error: "blue" is not "red" | "green"
```

## Generic classes and defaults

```ts
class TypedMap<K, V> {
  readonly #entries = new Map<K, V>();
  set(key: K, value: V): this { this.#entries.set(key, value); return this; }
  get(key: K): V | undefined { return this.#entries.get(key); }
}
```

Returning `this` keeps chains typed in subclasses.

## Generics and overloads

When a generic cannot express the relation between inputs and output (a type guard predicate
changes the result type, for instance), combine generics with **overloads**:

```ts
function partition<T, S extends T>(items: readonly T[], test: (item: T) => item is S): [S[], Exclude<T, S>[]];
function partition<T>(items: readonly T[], test: (item: T) => boolean): [T[], T[]];
function partition<T>(items: readonly T[], test: (item: T) => boolean): [T[], T[]] {
  const yes: T[] = [];
  const no: T[] = [];
  for (const item of items) (test(item) ? yes : no).push(item);
  return [yes, no];
}
```

`Exclude<T, S>` removes `S` from the union `T`; you will build such utility types yourself in
phase 2. You will write `partition` in the next kata.

## Rules of thumb

- Let inference fill type arguments; write them explicitly only when inference cannot.
- Constrain with the **least** you need (`{ length: number }`, `keyof T`), and return `T`.
- Do not add type parameters that do not relate two things.
- A cast inside a generic function (`{} as Record<K, V>`) is sometimes necessary; keep it in
  one place and make the public signature honest.

## Summary

- Type parameters keep information that `unknown` would lose; inference fills them per call.
- `extends` constrains; `keyof T` and `T[K]` relate keys and values; `typeof` turns values into types.
- `const` type parameters keep literals; `NoInfer` blocks inference from an argument.
- Generic classes, default type arguments, and overloads when the output depends on a predicate.

## Further reading

- TypeScript Handbook, "Generics": <https://www.typescriptlang.org/docs/handbook/2/generics.html>
- TypeScript Handbook, "Keyof Type Operator": <https://www.typescriptlang.org/docs/handbook/2/keyof-types.html>
- TypeScript Handbook, "Indexed Access Types": <https://www.typescriptlang.org/docs/handbook/2/indexed-access-types.html>
- TypeScript 5.0 release notes, `const` type parameters: <https://www.typescriptlang.org/docs/handbook/release-notes/typescript-5-0.html>
- TypeScript 5.4 release notes, `NoInfer`: <https://www.typescriptlang.org/docs/handbook/release-notes/typescript-5-4.html>
- roadmap.sh, TypeScript topic "Generic Constraints": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/typescript/content/generic-constraints@0NXHAOUYGIwuPwlN7r_B3.md>
