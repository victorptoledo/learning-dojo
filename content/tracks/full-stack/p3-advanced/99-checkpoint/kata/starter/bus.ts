// A pub/sub channel shared by every API instance: what Redis pub/sub or PostgreSQL
// LISTEN/NOTIFY does in production. Written for you.
export type Listener = (message: string) => void;

export class Bus {
  readonly #listeners = new Map<string, Set<Listener>>();

  /** Sends `message` to every subscriber of `channel`, on every instance, a moment later. */
  publish(channel: string, message: string): void {
    const listeners = [...(this.#listeners.get(channel) ?? [])];
    setImmediate(() => {
      for (const listener of listeners) listener(message);
    });
  }

  /** Subscribes and returns the function that unsubscribes. */
  subscribe(channel: string, listener: Listener): () => void {
    let listeners = this.#listeners.get(channel);
    if (!listeners) {
      listeners = new Set();
      this.#listeners.set(channel, listeners);
    }
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
      if (listeners.size === 0) this.#listeners.delete(channel);
    };
  }

  listenerCount(channel: string): number {
    return this.#listeners.get(channel)?.size ?? 0;
  }
}
