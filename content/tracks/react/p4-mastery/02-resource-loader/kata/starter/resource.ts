export type ResourceCache<T> = {
  /** The promise for `key`: the same one on every call until it is evicted. */
  read: (key: string) => Promise<T>;
  /** Forgets `key`, so the next read loads it again. */
  evict: (key: string) => void;
};

export function createResourceCache<T>(_load: (key: string) => Promise<T>): ResourceCache<T> {
  throw new Error("todo: write createResourceCache");
}
