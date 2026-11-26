// Overrides come from outside (a JSON file, an HTTP endpoint), so they are validated with a
// zod schema built from the definitions.
import { z } from "zod";
import type { Definitions, FlagDefinition, FlagValues } from "./definitions.ts";

export type Overrides<D extends Definitions> = Partial<FlagValues<D>>;

export type OverridesResult<D extends Definitions> =
  | { ok: true; overrides: Overrides<D> }
  | { ok: false; issues: string[] };

function schemaFor(definition: FlagDefinition): z.ZodType {
  switch (definition.kind) {
    case "boolean":
      return z.boolean();
    case "number": {
      let schema = z.number();
      if (definition.min !== undefined) schema = schema.min(definition.min);
      if (definition.max !== undefined) schema = schema.max(definition.max);
      return schema;
    }
    case "variant":
      return z.enum(definition.variants);
  }
}

function describe(issue: z.core.$ZodIssue): string[] {
  if (issue.code === "unrecognized_keys") {
    return issue.keys.map((key) => `${key}: unknown flag`);
  }
  return [`${issue.path.map(String).join(".") || "(root)"}: ${issue.message}`];
}

/** Validates untrusted overrides against the definitions: every key optional, no other keys. */
export function parseOverrides<D extends Definitions>(definitions: D, input: unknown): OverridesResult<D> {
  const shape: Record<string, z.ZodType> = {};
  for (const [name, definition] of Object.entries(definitions)) {
    shape[name] = schemaFor(definition).optional();
  }
  const parsed = z.strictObject(shape).safeParse(input);
  if (!parsed.success) {
    return { ok: false, issues: parsed.error.issues.flatMap(describe) };
  }
  // The one cast: the schema was built key by key from the same definitions.
  return { ok: true, overrides: parsed.data as Overrides<D> };
}
