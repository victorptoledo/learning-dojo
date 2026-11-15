import type { Config, ConfigInput, UserId } from "../config.ts";
import { parseUserId } from "../config.ts";
import type { Equal, Expect } from "./expect.ts";

// test: Config is inferred from the schema
export type Port = Expect<Equal<Config["server"]["port"], number>>;
export type Level = Expect<Equal<Config["logLevel"], "debug" | "info" | "warn" | "error">>;
export type Features = Expect<Equal<Config["features"], string[]>>;

// test: fields with defaults are optional in the input and required in the output
export type InputLevel = Expect<Equal<ConfigInput["logLevel"], "debug" | "info" | "warn" | "error" | undefined>>;
export type PoolSize = Expect<Equal<Config["database"]["poolSize"], number>>;

// test: a UserId is branded, so a plain string is not one
// @ts-expect-error: a plain string is not a UserId
export const fake: UserId = "usr_ab12cd34";
export const real: UserId | null = parseUserId("usr_ab12cd34");
export const asText: string | null = real;
