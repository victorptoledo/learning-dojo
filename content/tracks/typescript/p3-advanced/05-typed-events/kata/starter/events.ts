// A typed event emitter: the Events type maps each event name to its payload, and the
// compiler rejects unknown names and wrong payloads. The starter's signatures are loose
// (string, unknown): make them precise with keyof, indexed access and mapped types.

export type Listener<Payload> = (payload: Payload) => void;

/** todo: for { login: User }, { onLogin?: (payload: User) => void }, one optional handler per event. */
export type Handlers<Events> = Record<string, Listener<unknown>>;

export class TypedEmitter<Events extends Record<string, unknown>> {
  /** Adds a listener; returns a function that removes it. */
  on(name: string, listener: Listener<unknown>): () => void {
    throw new Error("todo: write on()");
  }

  /** Adds a listener that is removed before its first call. */
  once(name: string, listener: Listener<unknown>): () => void {
    throw new Error("todo: write once()");
  }

  /** Removes one registration of the listener, if it is there. */
  off(name: string, listener: Listener<unknown>): void {
    throw new Error("todo: write off()");
  }

  /**
   * Calls the listeners of the event in the order they were added and returns how many were
   * called. When some throw, the others still run, then an AggregateError with their errors.
   */
  emit(name: string, payload: unknown): number {
    throw new Error("todo: write emit()");
  }

  /** How many listeners the event has. */
  listenerCount(name: string): number {
    throw new Error("todo: write listenerCount()");
  }

  /** Adds one listener per handler: onLogin listens to "login". Returns a function that removes them all. */
  onAll(handlers: Handlers<Events>): () => void {
    throw new Error("todo: write onAll()");
  }

  /** Resolves with the next payload of the event; rejects with signal.reason when aborted. */
  waitFor(name: string, options: { signal?: AbortSignal } = {}): Promise<unknown> {
    throw new Error("todo: write waitFor()");
  }
}
