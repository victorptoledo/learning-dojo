import { chunk, filter, map, pairwise, range, Seq, zip } from "../seq.ts";
import type { Equal, Expect } from "./expect.ts";

// test: map and chunk keep the element types
const lengths = map(["a", "bb"], (text) => text.length);
export type Lengths = Expect<Equal<typeof lengths, Generator<number, void, undefined>>>;
const groups = chunk(range(0, 4), 2);
export type Groups = Expect<Equal<typeof groups, Generator<number[], void, undefined>>>;

// test: filter narrows with a type guard
declare const mixed: (string | number)[];
const strings = filter(mixed, (value): value is string => typeof value === "string");
export type Strings = Expect<Equal<typeof strings, Generator<string, void, undefined>>>;
const big = filter(mixed, (value) => value !== "");
export type Big = Expect<Equal<typeof big, Generator<string | number, void, undefined>>>;

// test: zip gives one type per source
const zipped = zip([1, 2], ["a", "b"], [true]);
export type Zipped = Expect<Equal<typeof zipped, Generator<[number, string, boolean], void, undefined>>>;

// test: pairwise gives pairs
const pairs = pairwise(["x"]);
export type Pairs = Expect<Equal<typeof pairs, Generator<[string, string], void, undefined>>>;

// test: Seq keeps and narrows its element type
const seq = Seq.from(mixed).filter((value): value is number => typeof value === "number").map(String);
export type SeqType = Expect<Equal<typeof seq, Seq<string>>>;
export type SeqArray = Expect<Equal<ReturnType<typeof seq.toArray>, string[]>>;
// @ts-expect-error: Seq.from takes an iterable
Seq.from(42);
