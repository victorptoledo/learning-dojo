// Runs an async function over a source with limited concurrency, yielding results in source
// order and pulling from the source only as fast as results are consumed.

export type ConcurrentOptions = { concurrency: number; signal?: AbortSignal };

export async function* mapConcurrent<T, U>(
  source: AsyncIterable<T> | Iterable<T>,
  fn: (item: T, signal: AbortSignal) => Promise<U>,
  options: ConcurrentOptions,
): AsyncGenerator<U, void, undefined> {
  // TODO: this runs one call at a time, ignores the options and never cancels anything.
  const controller = new AbortController();
  void options;
  for await (const item of source) {
    yield await fn(item, controller.signal);
  }
}
