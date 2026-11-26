// Flag definitions are plain constants; every type of the SDK is derived from them.
import { FlagsError } from "./errors.ts";

export type FlagDefinition =
  | { readonly kind: "boolean"; readonly default: boolean }
  | { readonly kind: "number"; readonly default: number; readonly min?: number; readonly max?: number }
  | { readonly kind: "variant"; readonly variants: readonly [string, ...string[]]; readonly default: string };

export type Definitions = Readonly<Record<string, FlagDefinition>>;

/** The value type of one flag: boolean, number, or the union of its variants. */
export type FlagValue<F extends FlagDefinition> = F extends { readonly kind: "boolean" }
  ? boolean
  : F extends { readonly kind: "number" }
    ? number
    : F extends { readonly variants: readonly (infer V extends string)[] }
      ? V
      : never;

export type FlagKey<D extends Definitions> = keyof D & string;

export type FlagValues<D extends Definitions> = { readonly [K in FlagKey<D>]: FlagValue<D[K]> };

/** One [key, value] pair, with the value type of that key. */
export type FlagEntry<D extends Definitions> = { [K in FlagKey<D>]: readonly [K, FlagValue<D[K]>] }[FlagKey<D>];

/** For variant flags, the default must be one of the variants. */
type CheckDefaults<D> = {
  [K in keyof D]: D[K] extends { readonly kind: "variant"; readonly variants: readonly (infer V)[] }
    ? { readonly default: V }
    : unknown;
};

const NAME = /^[a-z][A-Za-z0-9]*$/;

/** Checks the definitions (at compile time where it can, at runtime always) and returns them. */
export function defineFlags<const D extends Definitions>(definitions: D & CheckDefaults<D>): D {
  for (const [name, definition] of Object.entries(definitions)) {
    if (!NAME.test(name)) {
      throw new FlagsError(`invalid flag name: ${name}`);
    }
    if (definition.kind === "variant" && !definition.variants.includes(definition.default)) {
      throw new FlagsError(`${name}: the default is not one of the variants`);
    }
    if (definition.kind === "number") {
      const { min = -Infinity, max = Infinity } = definition;
      if (min > max || definition.default < min || definition.default > max) {
        throw new FlagsError(`${name}: the default is out of range`);
      }
    }
  }
  return definitions;
}
