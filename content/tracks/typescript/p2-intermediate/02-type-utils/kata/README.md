# Type Utilities

This kata has **no runtime code**: every test is type-level. `types.ts` declares ten types as
`unknown`; replace each one with a real definition. Do not use the built-in type that a
utility imitates (`Readonly` for `DeepReadonly`, `Awaited` for `UnwrapPromise`, `Omit` for
`Merge`); `Pick`, `Capitalize` and the other built-ins are fine.

| Type | Example |
| --- | --- |
| `DeepReadonly<T>` | Every property `readonly` through nested objects; arrays become `readonly X[]`; functions stay as they are |
| `Mutable<T>` | `Mutable<{ readonly x: number }>` is `{ x: number }` (optional stays optional) |
| `PickByValue<T, V>` | `PickByValue<{ a: string; b: number }, string>` is `{ a: string }` |
| `OptionalKeys<T>` | `OptionalKeys<{ a?: 1; b: 2 }>` is `"a"` (`never` when none) |
| `ElementOf<T>` | `ElementOf<string[]>` is `string`, `ElementOf<readonly [1, "two"]>` is `1 \| "two"`, otherwise `never` |
| `UnwrapPromise<T>` | `UnwrapPromise<Promise<Promise<string>>>` is `string`; a non-promise stays itself |
| `Getters<T>` | `Getters<{ name: string }>` is `{ getName: () => string }`, only for string keys |
| `Merge<A, B>` | One **flat** object type with the keys of both; the type from `B` wins |
| `RouteParams<P>` | `RouteParams<"/users/:id/posts/:postId">` is `{ id: string; postId: string }` |
| `PathKeys<T>` | `PathKeys<{ a: { b: number }; c: string }>` is `"a" \| "a.b" \| "c"` |

The tools: mapped types (`[K in keyof T]`), modifiers (`-readonly`, `-?`), key remapping
(`as`), conditional types with `infer`, recursion, and template literal types.

`Expect<Equal<A, B>>` (from `tests/expect.ts`) checks that two types are **identical**, so
`{ a: 1 } & { b: 2 }` does not equal `{ a: 1; b: 2 }`: build `Merge` as a single mapped type.

Run the checks with `npm test` (it runs `tsc`) after `npm install`. There are 12 tests:
1 in `tests/visible.types.ts` and 11 hidden ones, one per `// test:` section.
