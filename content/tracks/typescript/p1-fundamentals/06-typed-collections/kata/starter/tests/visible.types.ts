import { pluck } from "../collections.ts";
import type { Equal, Expect } from "./expect.ts";

const users = [
  { id: 1, name: "Ada" },
  { id: 2, name: "Grace" },
];

// test: pluck returns the type of the property
const names = pluck(users, "name");
export type PluckNames = Expect<Equal<typeof names, string[]>>;
