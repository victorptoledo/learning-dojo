// Counts renders so the tests can check the render budget. Do not change this file; call
// trackRender(`row:<ticket id>`) once in every render of a ticket row.
const counts = new Map<string, number>();

/** Records one render (or call) of `name`. */
export function trackRender(name: string): void {
  counts.set(name, (counts.get(name) ?? 0) + 1);
}

/** How many times `name` was recorded since the last reset. */
export function renderCount(name: string): number {
  return counts.get(name) ?? 0;
}

/** The total of every name that starts with `prefix`. */
export function renderTotal(prefix: string): number {
  let total = 0;
  for (const [name, count] of counts) {
    if (name.startsWith(prefix)) {
      total += count;
    }
  }
  return total;
}

export function resetRenders(): void {
  counts.clear();
}
