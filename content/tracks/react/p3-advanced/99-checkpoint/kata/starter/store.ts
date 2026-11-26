export type Listener = () => void;

export type Store<T> = {
  /** The current state. */
  getState(): T;
  /** Replaces the state, or computes it from the current one. */
  setState(next: T | ((current: T) => T)): void;
  /** Calls `listener` after every change; returns a function that stops it. */
  subscribe(listener: Listener): () => void;
};

/** A small external store. `T` is never a function. */
export function createStore<T>(_initial: T): Store<T> {
  throw new Error("todo: write createStore");
}

/** The part of the store's state that `selector` picks; re-renders when that part changes. */
export function useStore<T, S>(_store: Store<T>, _selector: (state: T) => S): S {
  throw new Error("todo: write useStore");
}
