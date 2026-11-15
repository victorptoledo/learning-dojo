import type {
  DeepReadonly,
  ElementOf,
  Getters,
  Merge,
  Mutable,
  OptionalKeys,
  PathKeys,
  PickByValue,
  RouteParams,
  UnwrapPromise,
} from "../types.ts";
import type { Equal, Expect } from "./expect.ts";

type User = {
  id: number;
  name: string;
  email?: string;
  tags: string[];
  address: { city: string; geo: { lat: number } };
  greet: () => string;
};

// test: DeepReadonly makes nested objects and arrays readonly
export type DeepUser = Expect<
  Equal<
    DeepReadonly<User>,
    {
      readonly id: number;
      readonly name: string;
      readonly email?: string;
      readonly tags: readonly string[];
      readonly address: { readonly city: string; readonly geo: { readonly lat: number } };
      readonly greet: () => string;
    }
  >
>;

// test: DeepReadonly forbids writes deep inside
declare const frozen: DeepReadonly<User>;
// reading still works
export const lat: number = frozen.address.geo.lat;
// @ts-expect-error: nested properties are readonly
frozen.address.geo.lat = 1;
// @ts-expect-error: arrays are readonly too
frozen.tags.push("x");

// test: Mutable keeps optional properties optional
export type MutableOptional = Expect<
  Equal<Mutable<{ readonly a?: string; readonly b: number }>, { a?: string; b: number }>
>;

// test: PickByValue keeps properties by value type
export type Strings = Expect<Equal<PickByValue<User, string>, { name: string }>>;
export type Functions = Expect<Equal<PickByValue<User, () => unknown>, { greet: () => string }>>;

// test: OptionalKeys finds the optional keys
export type Optional = Expect<Equal<OptionalKeys<User>, "email">>;
export type NoOptional = Expect<Equal<OptionalKeys<{ a: 1 }>, never>>;
export type Several = Expect<Equal<OptionalKeys<{ a?: 1; b: 2; c?: 3 }>, "a" | "c">>;

// test: ElementOf reads arrays and tuples
export type FromArray = Expect<Equal<ElementOf<string[]>, string>>;
export type FromTuple = Expect<Equal<ElementOf<readonly [1, "two"]>, 1 | "two">>;
export type NotAnArray = Expect<Equal<ElementOf<string>, never>>;

// test: UnwrapPromise unwraps nested promises
export type Once = Expect<Equal<UnwrapPromise<Promise<number>>, number>>;
export type Twice = Expect<Equal<UnwrapPromise<Promise<Promise<string[]>>>, string[]>>;
export type Plain = Expect<Equal<UnwrapPromise<boolean>, boolean>>;

// test: Getters builds one getter per string key
export type UserGetters = Expect<
  Equal<
    Getters<{ name: string; age: number; [Symbol.iterator]: () => void }>,
    { getName: () => string; getAge: () => number }
  >
>;

// test: Merge is flat and lets B win
export type Merged = Expect<
  Equal<Merge<{ a: number; b: string }, { b: boolean; c: null }>, { a: number; b: boolean; c: null }>
>;

// test: RouteParams reads the parameters of a path
export type Two = Expect<
  Equal<RouteParams<"/users/:id/posts/:postId">, { id: string; postId: string }>
>;
export type One = Expect<Equal<RouteParams<"/users/:id">, { id: string }>>;
export type None = Expect<Equal<RouteParams<"/health">, {}>>;

// test: PathKeys lists every dotted path
export type Paths = Expect<
  Equal<PathKeys<{ a: { b: number; c: { d: string } }; e: boolean }>, "a" | "a.b" | "a.c" | "a.c.d" | "e">
>;
export type Flat = Expect<Equal<PathKeys<{ x: 1 }>, "x">>;
