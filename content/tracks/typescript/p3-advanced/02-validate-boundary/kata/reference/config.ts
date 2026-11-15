// Validating configuration at the boundary of the program with zod: the schema is the single
// source of truth, and the TypeScript types are inferred from it.
import { z } from "zod";

export type Result<T, E> = { ok: true; value: T } | { ok: false; error: E };

export const ConfigSchema = z.object({
  server: z.object({
    host: z.string().min(1).default("0.0.0.0"),
    port: z.number().int().min(1).max(65535),
  }),
  database: z.object({
    url: z.url(),
    poolSize: z.number().int().positive().default(10),
  }),
  logLevel: z.enum(["debug", "info", "warn", "error"]).default("info"),
  features: z.array(z.string()).default([]),
});

/** The configuration after parsing: defaults filled in. */
export type Config = z.infer<typeof ConfigSchema>;

/** What a caller may pass in: fields with defaults are optional. */
export type ConfigInput = z.input<typeof ConfigSchema>;

export const UserIdSchema = z
  .string()
  .regex(/^usr_[a-z0-9]{8}$/)
  .brand<"UserId">();

export type UserId = z.infer<typeof UserIdSchema>;

function messages(error: z.ZodError): string[] {
  return error.issues.map((issue) => `${issue.path.map(String).join(".")}: ${issue.message}`);
}

/** Validates untrusted data; the errors are "path: message", for example "server.port: ...". */
export function parseConfig(input: unknown): Result<Config, string[]> {
  const parsed = ConfigSchema.safeParse(input);
  return parsed.success ? { ok: true, value: parsed.data } : { ok: false, error: messages(parsed.error) };
}

/** Parses JSON text, then validates it; ["invalid JSON"] when the text is not JSON. */
export function parseConfigJson(text: string): Result<Config, string[]> {
  let data: unknown;
  try {
    data = JSON.parse(text);
  } catch {
    return { ok: false, error: ["invalid JSON"] };
  }
  return parseConfig(data);
}

/** A number from an environment variable: undefined stays undefined, so defaults apply. */
function numberFrom(text: string | undefined): number | undefined {
  if (text === undefined) {
    return undefined;
  }
  return text.trim() === "" ? Number.NaN : Number(text);
}

/** Builds the configuration from environment variables (see README.md), then validates it. */
export function configFromEnv(env: Readonly<Record<string, string | undefined>>): Result<Config, string[]> {
  return parseConfig({
    server: { host: env.APP_HOST, port: numberFrom(env.APP_PORT) },
    database: { url: env.DATABASE_URL, poolSize: numberFrom(env.DB_POOL_SIZE) },
    logLevel: env.LOG_LEVEL,
    features: env.FEATURES?.split(",")
      .map((feature) => feature.trim())
      .filter((feature) => feature !== ""),
  });
}

/** A UserId, or null when the text is not a valid one. */
export function parseUserId(text: string): UserId | null {
  const parsed = UserIdSchema.safeParse(text);
  return parsed.success ? parsed.data : null;
}
