// A typed event emitter: the Events type maps each event name to its payload, and the
// compiler rejects unknown names and wrong payloads. The starter's signatures are loose
// (string, unknown): make them precise with keyof, indexed access and mapped types.

export type Listener<Payload> = (payload: Payload) => void;

/** For { login: User }, { onLogin?: (payload: User) => void }, one optional handler per event. */
export type Handlers<Events> = {
  [K in keyof Events & string as `on${Capitalize<K>}`]?: Listener<Events[K]>;
};

export class TypedEmitter<Events extends Record<string, unknown>> {
  readonly #listeners = new Map<keyof Events, Listener<never>[]>();

  #list<K extends keyof Events>(name: K): Listener<Events[K]>[] {
    let list = this.#listeners.get(name) as Listener<Events[K]>[] | undefined;
    if (list === undefined) {
      list = [];
      this.#listeners.set(name, list);
    }
    return list;
  }

  /** Adds a listener; returns a function that removes it. */
  on<K extends keyof Events>(name: K, listener: Listener<Events[K]>): () => void {
    this.#list(name).push(listener);
    let active = true;
    return () => {
      if (active) {
        active = false;
        this.off(name, listener);
      }
    };
  }

  /** Adds a listener that is removed before its first call. */
  once<K extends keyof Events>(name: K, listener: Listener<Events[K]>): () => void {
    const stop = this.on(name, (payload) => {
      stop();
      listener(payload);
    });
    return stop;
  }

  /** Removes one registration of the listener, if it is there. */
  off<K extends keyof Events>(name: K, listener: Listener<Events[K]>): void {
    const list = this.#list(name);
    const index = list.indexOf(listener);
    if (index !== -1) {
      list.splice(index, 1);
    }
  }

  /**
   * Calls the listeners of the event in the order they were added and returns how many were
   * called. When some throw, the others still run, then an AggregateError with their errors.
   */
  emit<K extends keyof Events>(name: K, payload: Events[K]): number {
    const listeners = [...this.#list(name)];
    const errors: unknown[] = [];
    for (const listener of listeners) {
      try {
        listener(payload);
      } catch (error) {
        errors.push(error);
      }
    }
    if (errors.length > 0) {
      throw new AggregateError(errors, `${errors.length} listeners failed for "${String(name)}"`);
    }
    return listeners.length;
  }

  /** How many listeners the event has. */
  listenerCount(name: keyof Events): number {
    return this.#listeners.get(name)?.length ?? 0;
  }

  /** Adds one listener per handler: onLogin listens to "login". Returns a function that removes them all. */
  onAll(handlers: Handlers<Events>): () => void {
    const stops: (() => void)[] = [];
    for (const [key, handler] of Object.entries(handlers)) {
      if (typeof handler === "function" && key.startsWith("on") && key.length > 2) {
        const name = key[2]?.toLowerCase() + key.slice(3);
        stops.push(this.on(name as keyof Events, handler as Listener<Events[keyof Events]>));
      }
    }
    return () => {
      for (const stop of stops) {
        stop();
      }
    };
  }

  /** Resolves with the next payload of the event; rejects with signal.reason when aborted. */
  waitFor<K extends keyof Events>(name: K, options: { signal?: AbortSignal } = {}): Promise<Events[K]> {
    const { signal } = options;
    return new Promise((resolve, reject) => {
      if (signal?.aborted) {
        reject(signal.reason);
        return;
      }
      const onAbort = () => {
        stop();
        reject(signal?.reason);
      };
      const stop = this.once(name, (payload) => {
        signal?.removeEventListener("abort", onAbort);
        resolve(payload);
      });
      signal?.addEventListener("abort", onAbort, { once: true });
    });
  }
}
