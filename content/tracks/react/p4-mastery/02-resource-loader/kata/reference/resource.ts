export type ResourceCache<T> = {
  /** The promise for `key`: the same one on every call until it is evicted. */
  read: (key: string) => Promise<T>;
  /** Forgets `key`, so the next read loads it again. */
  evict: (key: string) => void;
};

export function createResourceCache<T>(load: (key: string) => Promise<T>): ResourceCache<T> {
  const promises = new Map<string, Promise<T>>();
  return {
    read(key) {
      let promise = promises.get(key);
      if (promise === undefined) {
        promise = load(key);
        promises.set(key, promise);
      }
      return promise;
    },
    evict(key) {
      promises.delete(key);
    },
  };
}
