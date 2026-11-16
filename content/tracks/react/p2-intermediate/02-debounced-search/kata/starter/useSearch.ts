export type SearchFn = (query: string, signal: AbortSignal) => Promise<readonly string[]>;

export type SearchState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; results: readonly string[] }
  | { status: "error"; message: string };

/** Searches for the debounced, trimmed query; aborts and ignores stale searches. */
export function useSearch(_query: string, _search: SearchFn, _delayMs = 300): SearchState {
  throw new Error("todo: write useSearch");
}
