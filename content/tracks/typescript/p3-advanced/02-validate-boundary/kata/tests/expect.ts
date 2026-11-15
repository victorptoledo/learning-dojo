// Helpers for type-level tests: `Expect<Equal<A, B>>` compiles only when A and B are the same type.

export type Equal<A, B> =
  (<T>() => T extends A ? 1 : 2) extends <T>() => T extends B ? 1 : 2 ? true : false;

export type Expect<T extends true> = T;
