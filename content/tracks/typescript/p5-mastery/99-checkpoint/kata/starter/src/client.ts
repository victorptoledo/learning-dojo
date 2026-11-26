// The flag client: typed reads, listeners per flag and refreshes that apply all or nothing.
import type { Definitions, FlagEntry, FlagKey, FlagValue, FlagValues } from "./definitions.ts";
import type { Overrides } from "./overrides.ts";

// TODO: this "method" form is checked bivariantly, so a listener for one variant only is
// accepted for a flag with two. Make it sound.
export type Listener<V> = { bivarianceHack(value: V, previous: V): void }["bivarianceHack"];

export type RefreshOptions = { timeoutMs?: number };
export type RefreshResult<D extends Definitions> = { changed: FlagKey<D>[]; stale: boolean };

export class FlagClient<D extends Definitions> {
  readonly #definitions: D;
  #overrides: Overrides<D>;

  constructor(definitions: D, overrides: unknown = {}) {
    this.#definitions = definitions;
    this.#overrides = overrides as Overrides<D>; // TODO: validate
  }

  get<K extends FlagKey<D>>(key: K): FlagValue<D[K]> {
    return (this.#overrides[key] ?? this.#definitions[key]?.default) as FlagValue<D[K]>;
  }

  /** A frozen copy of every value. */
  snapshot(): FlagValues<D> {
    throw new Error("TODO: snapshot");
  }

  /** [key, value] pairs, in the order of the definitions. */
  entries(): Generator<FlagEntry<D>, void, undefined> {
    throw new Error("TODO: entries");
  }

  subscribe<K extends FlagKey<D>>(key: K, listener: Listener<FlagValue<D[K]>>): () => void {
    void key;
    void listener;
    return () => {};
  }

  /**
   * Loads new overrides and applies them all at once. A refresh that finishes after a newer one
   * started is stale and changes nothing.
   */
  async refresh(
    load: (signal: AbortSignal) => Promise<unknown>,
    options: RefreshOptions = {},
  ): Promise<RefreshResult<D>> {
    void options;
    this.#overrides = (await load(new AbortController().signal)) as Overrides<D>;
    return { changed: [], stale: false };
  }
}

export function createClient<D extends Definitions>(definitions: D, options: { overrides?: Overrides<D> } = {}): FlagClient<D> {
  return new FlagClient(definitions, options.overrides ?? {});
}
