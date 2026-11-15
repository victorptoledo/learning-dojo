# Utility, Conditional, Mapped and Template Literal Types

TypeScript's type system is a small programming language of its own. Its values are types,
and it has functions (generic types), conditionals, loops over keys and string templates. You
do not need it every day, but you will **read** it every day in library typings, and a few
derived types remove a lot of duplication from application code.

## The built-in utility types

| Utility | Gives | Example |
| --- | --- | --- |
| `Partial<T>` | Every property optional | Patch objects: `update(id, changes: Partial<User>)` |
| `Required<T>` | Every property required | After defaults are applied |
| `Readonly<T>` | Every property readonly (one level) | Frozen config |
| `Pick<T, K>` | Only the keys `K` | `Pick<User, "id" \| "name">` |
| `Omit<T, K>` | All keys but `K` | `Omit<User, "password">` |
| `Record<K, V>` | An object with keys `K` and values `V` | `Record<Status, number>` |
| `Exclude<U, X>` / `Extract<U, X>` | Union members not assignable / assignable to `X` | `Exclude<Status, "draft">` |
| `NonNullable<T>` | `T` without `null` and `undefined` | |
| `ReturnType<F>` / `Parameters<F>` | A function's return / parameter tuple | Type a wrapper from the wrapped |
| `Awaited<T>` | What a promise resolves to, recursively | `Awaited<ReturnType<typeof load>>` |
| `InstanceType<C>` | The instance type of a class | Factories |

Derive types from one source instead of repeating fields:

```ts
interface User { id: number; name: string; email: string; password: string }

type PublicUser = Omit<User, "password">;
type NewUser = Omit<User, "id">;
type UserPatch = Partial<NewUser>;
```

`Omit` does not check that the key exists (`Omit<User, "pasword">` compiles); `Pick` does.

## Conditional types

```ts
type IsString<T> = T extends string ? true : false;
type A = IsString<"hi">;   // true
type B = IsString<42>;     // false
```

`T extends U ? X : Y` reads "if `T` is assignable to `U`". Used with a union, a conditional
type is **distributive**: it is applied to each member, and the results are joined.

```ts
type Exclude<U, X> = U extends X ? never : U;   // this is how the built-in is defined
type Status = "draft" | "live" | "archived";
type Visible = Exclude<Status, "draft">;        // "live" | "archived"
```

Returning `never` for a member drops it, which is how conditional types **filter** unions. To
stop distribution, wrap both sides in a tuple: `[T] extends [U] ? ... : ...`.

### infer

`infer` declares a type variable inside the `extends` clause and captures whatever matches:

```ts
type ElementOf<T> = T extends readonly (infer Item)[] ? Item : never;
type Unwrap<T> = T extends Promise<infer V> ? V : T;
type FirstArg<F> = F extends (first: infer A, ...rest: never[]) => unknown ? A : never;

type E = ElementOf<string[]>;          // string
type U = Unwrap<Promise<number>>;      // number
```

`ReturnType`, `Parameters` and `Awaited` are all written this way. Conditional types may be
**recursive**: `Unwrap` can call itself on `V` to unwrap nested promises.

## Mapped types

A mapped type loops over keys and builds an object type:

```ts
type Flags<T> = { [K in keyof T]: boolean };
type Readonly<T> = { readonly [K in keyof T]: T[K] };
type Mutable<T> = { -readonly [K in keyof T]: T[K] };   // - removes a modifier
type Concrete<T> = { [K in keyof T]-?: T[K] };          // -? removes optionality
```

Mapping over `keyof T` keeps the original modifiers (readonly, optional) unless you add or
remove them, which makes mapped types *homomorphic*.

### Key remapping with as

`as` rewrites each key; mapping a key to `never` removes it:

```ts
type Getters<T> = {
  [K in keyof T as `get${Capitalize<K & string>}`]: () => T[K];
};
type OnlyStrings<T> = {
  [K in keyof T as T[K] extends string ? K : never]: T[K];
};

type G = Getters<{ name: string }>;   // { getName: () => string }
```

## Template literal types

Template literals work on types too, and distribute over unions:

```ts
type Side = "top" | "bottom";
type Margin = `margin-${Side}`;               // "margin-top" | "margin-bottom"
type Event = `on${Capitalize<"click" | "focus">}`; // "onClick" | "onFocus"
```

The intrinsic helpers `Uppercase`, `Lowercase`, `Capitalize` and `Uncapitalize` transform
string literal types. With `infer`, template literals **parse** strings at the type level:

```ts
type Param<Path> = Path extends `${string}:${infer Name}/${infer Rest}`
  ? Name | Param<`/${Rest}`>
  : Path extends `${string}:${infer Name}` ? Name : never;

type P = Param<"/users/:id/posts/:postId">;   // "id" | "postId"
```

Routers and SQL builders type their APIs this way.

## Recursive types and their limits

```ts
type Json = string | number | boolean | null | Json[] | { [key: string]: Json };
type DeepReadonly<T> = T extends object ? { readonly [K in keyof T]: DeepReadonly<T[K]> } : T;
```

The compiler limits recursion depth (it reports "Type instantiation is excessively deep" around
a few dozen levels for some patterns, and caps tail-recursive conditional types at 1000).
Recursive types also slow the checker down. Use them where they buy real safety, and test them.

## Testing types

A type is code: test it. The usual trick is a helper that only compiles when two types are
identical, plus `@ts-expect-error` for things that must fail:

```ts
type Equal<A, B> = (<T>() => T extends A ? 1 : 2) extends <T>() => T extends B ? 1 : 2 ? true : false;
type Expect<T extends true> = T;

type Check = Expect<Equal<ElementOf<number[]>, number>>;
// @ts-expect-error: a non-array has no element type
const bad: ElementOf<string> = "x";
```

The next kata is graded entirely this way.

## When to stop

Type-level code is hard to read and to debug. Prefer, in this order: a plain type; a built-in
utility; a small mapped or conditional type with a good name; a recursive or parsing type only
in library code that many callers benefit from.

## Summary

- Derive types with `Partial`, `Pick`, `Omit`, `Record`, `ReturnType`, `Awaited` and friends.
- `T extends U ? X : Y` branches, distributes over unions and filters with `never`; `infer` captures.
- Mapped types loop over keys, add or remove `readonly` and `?`, and remap keys with `as`.
- Template literal types build and parse string types.
- Test type-level code with `Expect<Equal<...>>` and `@ts-expect-error`.

## Further reading

- TypeScript Handbook, "Utility Types": <https://www.typescriptlang.org/docs/handbook/utility-types.html>
- TypeScript Handbook, "Conditional Types": <https://www.typescriptlang.org/docs/handbook/2/conditional-types.html>
- TypeScript Handbook, "Mapped Types": <https://www.typescriptlang.org/docs/handbook/2/mapped-types.html>
- TypeScript Handbook, "Template Literal Types": <https://www.typescriptlang.org/docs/handbook/2/template-literal-types.html>
- roadmap.sh, TypeScript topic "Conditional Types": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/typescript/content/conditional-types@ubGIhi-WLE0EopTYXzmPL.md>
