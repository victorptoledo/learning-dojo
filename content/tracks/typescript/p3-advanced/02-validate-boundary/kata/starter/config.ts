// Validating configuration at the boundary of the program with zod: the schema is the single
// source of truth, and the TypeScript types are inferred from it.
import { z } from "zod";

export type Result<T, E> = { ok: true; value: T } | { ok: false; error: E };

/** todo: describe the configuration (see README.md). */
export const ConfigSchema = z.object({});

/** The configuration after parsing: defaults filled in. */
export type Config = z.infer<typeof ConfigSchema>;

/** What a caller may pass in: fields with defaults are optional. */
export type ConfigInput = z.input<typeof ConfigSchema>;

/** todo: a string like "usr_" followed by 8 lower-case letters or digits, branded as UserId. */
export const UserIdSchema = z.string();

export type UserId = z.infer<typeof UserIdSchema>;

/** Validates untrusted data; the errors are "path: message", for example "server.port: ...". */
export function parseConfig(input: unknown): Result<Config, string[]> {
  throw new Error("todo: write parseConfig()");
}

/** Parses JSON text, then validates it; ["invalid JSON"] when the text is not JSON. */
export function parseConfigJson(text: string): Result<Config, string[]> {
  throw new Error("todo: write parseConfigJson()");
}

/** Builds the configuration from environment variables (see README.md), then validates it. */
export function configFromEnv(env: Readonly<Record<string, string | undefined>>): Result<Config, string[]> {
  throw new Error("todo: write configFromEnv()");
}

/** A UserId, or null when the text is not a valid one. */
export function parseUserId(text: string): UserId | null {
  throw new Error("todo: write parseUserId()");
}
