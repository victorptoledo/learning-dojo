// Type-level utilities. Nothing here runs: the tests are all type-level (tests/*.types.ts).
// Replace each `unknown` (todo) with a type built from mapped, conditional and template
// literal types. Do not use the built-in Readonly, Awaited or Omit for the type they replace.

/** Every property readonly, all the way down through objects and arrays; functions untouched. */
export type DeepReadonly<T> = unknown; // todo

/** Removes `readonly` from the properties of T (one level). */
export type Mutable<T> = unknown; // todo

/** The properties of T whose value type is assignable to V. */
export type PickByValue<T, V> = unknown; // todo

/** The union of the keys of T that are optional. */
export type OptionalKeys<T> = unknown; // todo

/** The element type of an array or tuple; never for anything else. */
export type ElementOf<T> = unknown; // todo

/** The value a promise resolves to, through nested promises; T itself when it is not a promise. */
export type UnwrapPromise<T> = unknown; // todo

/** For { name: string }, { getName: () => string }: one getter per string key. */
export type Getters<T> = unknown; // todo

/** The properties of A and B in one flat object type; B wins when both have a key. */
export type Merge<A, B> = unknown; // todo

/** For "/users/:id/posts/:postId", { id: string; postId: string }; {} without parameters. */
export type RouteParams<Path extends string> = unknown; // todo

/** Every dotted path to a property: for { a: { b: number }; c: string }, "a" | "a.b" | "c". */
export type PathKeys<T> = unknown; // todo
