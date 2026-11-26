// The flag client: typed reads, listeners per flag and refreshes that apply all or nothing.
import type { Definitions, FlagEntry, FlagKey, FlagValue, FlagValues } from "./definitions.ts";
import { FlagsError } from "./errors.ts";
import { parseOverrides, type Overrides } from "./overrides.ts";

/** A function type, not a method: parameters are checked contravariantly. */
export type Listener<V> = (value: V, previous: V) => void;

export type RefreshOptions = { timeoutMs?: number };
export type RefreshResult<D extends Definitions> = { changed: FlagKey<D>[]; stale: boolean };

type AnyListener = (value: unknown, previous: unknown) => void;

/** Rejects with the signal's reason as soon as it aborts, whatever the promise does. */
function untilAborted<T>(promise: Promise<T>, signal: AbortSignal): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const onAbort = () => reject(signal.reason);
    if (signal.aborted) {
      onAbort();
      return;
    }
    signal.addEventListener("abort", onAbort, { once: true });
    promise.then(resolve, reject).finally(() => signal.removeEventListener("abort", onAbort));
  });
}

export class FlagClient<D extends Definitions> {
  readonly #definitions: D;
  readonly #keys: FlagKey<D>[];
  #overrides: Overrides<D>;
  readonly #listeners = new Map<string, Set<AnyListener>>();
  #latest = 0;

  constructor(definitions: D, overrides: unknown = {}) {
    this.#definitions = definitions;
    this.#keys = Object.keys(definitions) as FlagKey<D>[]; // the keys of D, in definition order
    const parsed = parseOverrides(definitions, overrides);
    if (!parsed.ok) {
      throw new FlagsError("invalid overrides", { issues: parsed.issues });
    }
    this.#overrides = parsed.overrides;
  }

  get<K extends FlagKey<D>>(key: K): FlagValue<D[K]> {
    const definition = Object.hasOwn(this.#definitions, key) ? this.#definitions[key] : undefined;
    if (definition === undefined) {
      throw new FlagsError(`unknown flag: ${String(key)}`);
    }
    return (this.#overrides[key] ?? definition.default) as FlagValue<D[K]>;
  }

  /** A frozen copy of every value. */
  snapshot(): FlagValues<D> {
    const values: Record<string, unknown> = {};
    for (const key of this.#keys) {
      values[key] = this.get(key);
    }
    return Object.freeze(values) as FlagValues<D>;
  }

  /** [key, value] pairs, in the order of the definitions. */
  *entries(): Generator<FlagEntry<D>, void, undefined> {
    for (const key of this.#keys) {
      yield [key, this.get(key)] as FlagEntry<D>;
    }
  }

  subscribe<K extends FlagKey<D>>(key: K, listener: Listener<FlagValue<D[K]>>): () => void {
    const stored = listener as AnyListener;
    const set = this.#listeners.get(key) ?? new Set<AnyListener>();
    set.add(stored);
    this.#listeners.set(key, set);
    return () => {
      set.delete(stored);
    };
  }

  /**
   * Loads new overrides and applies them all at once. A refresh that finishes after a newer one
   * started is stale and changes nothing.
   */
  async refresh(
    load: (signal: AbortSignal) => Promise<unknown>,
    options: RefreshOptions = {},
  ): Promise<RefreshResult<D>> {
    const id = ++this.#latest;
    const signal = AbortSignal.timeout(options.timeoutMs ?? 1_000);

    let payload: unknown;
    try {
      payload = await untilAborted(load(signal), signal);
    } catch (error) {
      throw new FlagsError("refresh failed: the overrides could not be loaded", { cause: error });
    }
    if (id !== this.#latest) {
      return { changed: [], stale: true };
    }

    const parsed = parseOverrides(this.#definitions, payload);
    if (!parsed.ok) {
      throw new FlagsError("refresh failed: invalid overrides", { issues: parsed.issues });
    }

    const before = this.snapshot();
    this.#overrides = parsed.overrides;
    const after = this.snapshot();
    const changed = this.#keys.filter((key) => before[key] !== after[key]);

    const errors: unknown[] = [];
    for (const key of changed) {
      for (const listener of [...(this.#listeners.get(key) ?? [])]) {
        try {
          listener(after[key], before[key]);
        } catch (error) {
          errors.push(error);
        }
      }
    }
    if (errors.length > 0) {
      throw new AggregateError(errors, "flag listeners failed");
    }
    return { changed, stale: false };
  }
}

export function createClient<D extends Definitions>(definitions: D, options: { overrides?: Overrides<D> } = {}): FlagClient<D> {
  return new FlagClient(definitions, options.overrides ?? {});
}
