import { createReadStream } from "node:fs";
import { mapConcurrent } from "../concurrent.ts";
import type { Level, Window } from "../logs.ts";
import { LEVELS, lines, parseEntries, windows } from "../logs.ts";
import type { Equal, Expect } from "./expect.ts";

// test: the level union comes from the LEVELS constant
export type Levels = Expect<Equal<Level, "debug" | "info" | "warn" | "error">>;
export type LevelList = Expect<Equal<typeof LEVELS, readonly ["debug", "info", "warn", "error"]>>;
export type Counts = Expect<Equal<Window["counts"], Record<Level, number>>>;

// test: the stages are typed async generators
const fromStream = lines(createReadStream("app.log"));
export type Lines = Expect<Equal<typeof fromStream, AsyncGenerator<string, void, undefined>>>;
const fromArray = windows(parseEntries(["x"]), 1000);
export type Windows = Expect<Equal<typeof fromArray, AsyncGenerator<Window, void, undefined>>>;
// @ts-expect-error: lines need text or bytes
lines([1, 2]);

// test: mapConcurrent infers its result type
const names = mapConcurrent([1, 2], async (n, signal) => `${n}${signal.aborted}`, { concurrency: 2 });
export type Names = Expect<Equal<typeof names, AsyncGenerator<string, void, undefined>>>;
// @ts-expect-error: concurrency is required
mapConcurrent([1], async (n) => n, {});
