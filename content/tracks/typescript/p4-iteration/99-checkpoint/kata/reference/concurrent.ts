// Runs an async function over a source with limited concurrency, yielding results in source
// order and pulling from the source only as fast as results are consumed.

export type ConcurrentOptions = { concurrency: number; signal?: AbortSignal };

async function* toAsync<T>(source: AsyncIterable<T> | Iterable<T>): AsyncGenerator<T, void, undefined> {
  yield* source;
}

export async function* mapConcurrent<T, U>(
  source: AsyncIterable<T> | Iterable<T>,
  fn: (item: T, signal: AbortSignal) => Promise<U>,
  options: ConcurrentOptions,
): AsyncGenerator<U, void, undefined> {
  const { concurrency, signal } = options;
  if (!Number.isInteger(concurrency) || concurrency < 1) {
    throw new RangeError("concurrency must be a positive integer");
  }
  signal?.throwIfAborted();

  const controller = new AbortController();
  const forward = () => controller.abort(signal?.reason);
  signal?.addEventListener("abort", forward, { once: true });

  const iterator = toAsync(source);
  const pending: Promise<U>[] = [];
  let exhausted = false;
  try {
    while (true) {
      while (!exhausted && pending.length < concurrency) {
        signal?.throwIfAborted();
        const next = await iterator.next();
        if (next.done) {
          exhausted = true;
          break;
        }
        const call = fn(next.value, controller.signal);
        call.catch(() => {}); // awaited later, in order; this only avoids an unhandled rejection
        pending.push(call);
      }
      const oldest = pending.shift();
      if (oldest === undefined) {
        return;
      }
      const value = await oldest;
      signal?.throwIfAborted();
      yield value;
    }
  } finally {
    signal?.removeEventListener("abort", forward);
    if (!controller.signal.aborted) {
      controller.abort(new Error("mapConcurrent stopped"));
    }
    await iterator.return();
  }
}
