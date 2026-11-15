// Type-level utilities. Nothing here runs: the tests are all type-level (tests/*.types.ts).
// Replace each `unknown` (todo) with a type built from mapped, conditional and template
// literal types. Do not use the built-in Readonly, Awaited or Omit for the type they replace.

/** Every property readonly, all the way down through objects and arrays; functions untouched. */
export type DeepReadonly<T> = T extends (...args: never[]) => unknown
  ? T
  : T extends readonly (infer Item)[]
    ? readonly DeepReadonly<Item>[]
    : T extends object
      ? { readonly [K in keyof T]: DeepReadonly<T[K]> }
      : T;

/** Removes `readonly` from the properties of T (one level). */
export type Mutable<T> = { -readonly [K in keyof T]: T[K] };

/** The properties of T whose value type is assignable to V. */
export type PickByValue<T, V> = { [K in keyof T as T[K] extends V ? K : never]: T[K] };

/** The union of the keys of T that are optional. */
export type OptionalKeys<T> = {
  [K in keyof T]-?: object extends Pick<T, K> ? K : never;
}[keyof T];

/** The element type of an array or tuple; never for anything else. */
export type ElementOf<T> = T extends readonly (infer Item)[] ? Item : never;

/** The value a promise resolves to, through nested promises; T itself when it is not a promise. */
export type UnwrapPromise<T> = T extends PromiseLike<infer Value> ? UnwrapPromise<Value> : T;

/** For { name: string }, { getName: () => string }: one getter per string key. */
export type Getters<T> = {
  [K in keyof T as K extends string ? `get${Capitalize<K>}` : never]: () => T[K];
};

/** The properties of A and B in one flat object type; B wins when both have a key. */
export type Merge<A, B> = {
  [K in keyof A | keyof B]: K extends keyof B ? B[K] : K extends keyof A ? A[K] : never;
};

/** For "/users/:id/posts/:postId", { id: string; postId: string }; {} without parameters. */
export type RouteParams<Path extends string> = {
  [K in ParamNames<Path>]: string;
};

type ParamNames<Path extends string> = Path extends `${string}:${infer Name}/${infer Rest}`
  ? Name | ParamNames<`/${Rest}`>
  : Path extends `${string}:${infer Name}`
    ? Name
    : never;

/** Every dotted path to a property: for { a: { b: number }; c: string }, "a" | "a.b" | "c". */
export type PathKeys<T> = {
  [K in keyof T & string]: T[K] extends object ? K | `${K}.${PathKeys<T[K]>}` : K;
}[keyof T & string];
