// Flag definitions are plain constants; every type of the SDK is derived from them.
import { FlagsError } from "./errors.ts";

export type FlagDefinition =
  | { readonly kind: "boolean"; readonly default: boolean }
  | { readonly kind: "number"; readonly default: number; readonly min?: number; readonly max?: number }
  | { readonly kind: "variant"; readonly variants: readonly [string, ...string[]]; readonly default: string };

export type Definitions = Readonly<Record<string, FlagDefinition>>;

/** The value type of one flag: boolean, number, or the union of its variants. */
// TODO: a conditional type on the definition's kind.
export type FlagValue<F extends FlagDefinition> = F["default"];

export type FlagKey<D extends Definitions> = keyof D & string;

export type FlagValues<D extends Definitions> = { readonly [K in FlagKey<D>]: FlagValue<D[K]> };

/** One [key, value] pair, with the value type of that key. */
// TODO: a union with one tuple per key, so that narrowing on the key narrows the value.
export type FlagEntry<D extends Definitions> = readonly [FlagKey<D>, unknown];

/** Checks the definitions (at compile time where it can, at runtime always) and returns them. */
// TODO: keep the literal types of the definitions, and reject a variant default that is not
// one of the variants at compile time too.
export function defineFlags<D extends Definitions>(definitions: D): D {
  if (Object.keys(definitions).length === 0) {
    throw new FlagsError("no flags defined");
  }
  return definitions;
}
