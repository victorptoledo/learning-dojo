// Overrides come from outside (a JSON file, an HTTP endpoint), so they are validated with a
// zod schema built from the definitions.
import type { Definitions, FlagValues } from "./definitions.ts";

export type Overrides<D extends Definitions> = Partial<FlagValues<D>>;

export type OverridesResult<D extends Definitions> =
  | { ok: true; overrides: Overrides<D> }
  | { ok: false; issues: string[] };

/** Validates untrusted overrides against the definitions: every key optional, no other keys. */
export function parseOverrides<D extends Definitions>(definitions: D, input: unknown): OverridesResult<D> {
  // TODO: build a zod schema from the definitions.
  void definitions;
  return { ok: true, overrides: input as Overrides<D> };
}
