import { chunk, indexBy, keyed, maxBy, partition, pluck } from "../collections.ts";
import type { Equal, Expect } from "./expect.ts";

type User = { id: number; name: string; admin: boolean };
const users: User[] = [{ id: 1, name: "Ada", admin: true }];

// test: pluck keeps the type of each property
const ids = pluck(users, "id");
export type PluckIds = Expect<Equal<typeof ids, number[]>>;

// test: pluck only accepts keys of the item
// @ts-expect-error: "email" is not a key of User
pluck(users, "email");

// test: indexBy keeps the key type
const byId = indexBy(users, (user) => user.id);
export type ById = Expect<Equal<typeof byId, Map<number, User>>>;

// test: partition with a type guard narrows the matching part
type Cat = { kind: "cat"; meows: boolean };
type Dog = { kind: "dog"; barks: boolean };
const pets: (Cat | Dog)[] = [];
const [cats, dogs] = partition(pets, (pet): pet is Cat => pet.kind === "cat");
export type Cats = Expect<Equal<typeof cats, Cat[]>>;
export type Dogs = Expect<Equal<typeof dogs, Dog[]>>;

// test: partition with a plain predicate keeps the item type
const [admins, others] = partition(users, (user) => user.admin);
export type Admins = Expect<Equal<typeof admins, User[]>>;
export type Others = Expect<Equal<typeof others, User[]>>;

// test: maxBy and chunk keep the item type
export type Best = Expect<Equal<ReturnType<typeof maxBy<User>>, User | undefined>>;
export type Chunks = Expect<Equal<ReturnType<typeof chunk<string>>, string[][]>>;

// test: keyed knows each key and the value type
const lengths = keyed(["a", "bb"], (key) => key.length);
export type Lengths = Expect<Equal<typeof lengths, Record<"a" | "bb", number>>>;
