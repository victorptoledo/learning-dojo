import type { Mutable } from "../types.ts";
import type { Equal, Expect } from "./expect.ts";

// test: Mutable removes readonly
export type MutablePoint = Expect<
  Equal<Mutable<{ readonly x: number; readonly y: number }>, { x: number; y: number }>
>;
