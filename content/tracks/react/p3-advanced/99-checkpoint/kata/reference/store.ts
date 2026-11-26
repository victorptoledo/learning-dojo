import { useSyncExternalStore } from "react";

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
export function createStore<T>(initial: T): Store<T> {
  let state = initial;
  const listeners = new Set<Listener>();
  return {
    getState: () => state,
    setState(next) {
      const value = typeof next === "function" ? (next as (current: T) => T)(state) : next;
      if (Object.is(value, state)) {
        return;
      }
      state = value;
      for (const listener of [...listeners]) {
        listener();
      }
    },
    subscribe(listener) {
      listeners.add(listener);
      return () => {
        listeners.delete(listener);
      };
    },
  };
}

/** The part of the store's state that `selector` picks; re-renders when that part changes. */
export function useStore<T, S>(store: Store<T>, selector: (state: T) => S): S {
  return useSyncExternalStore(store.subscribe, () => selector(store.getState()));
}
