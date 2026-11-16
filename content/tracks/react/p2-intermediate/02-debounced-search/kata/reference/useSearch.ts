import { useEffect, useRef, useState } from "react";
import { useDebouncedValue } from "./useDebouncedValue.ts";

export type SearchFn = (query: string, signal: AbortSignal) => Promise<readonly string[]>;

export type SearchState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; results: readonly string[] }
  | { status: "error"; message: string };

/** Searches for the debounced, trimmed query; aborts and ignores stale searches. */
export function useSearch(query: string, search: SearchFn, delayMs = 300): SearchState {
  const debounced = useDebouncedValue(query.trim(), delayMs);
  const [state, setState] = useState<SearchState>({ status: "idle" });

  // The latest search function, without making the effect below depend on it.
  const searchRef = useRef(search);
  useEffect(() => {
    searchRef.current = search;
  });

  useEffect(() => {
    if (debounced === "") {
      setState({ status: "idle" });
      return;
    }
    const controller = new AbortController();
    setState({ status: "loading" });
    searchRef.current(debounced, controller.signal).then(
      (results) => {
        if (!controller.signal.aborted) {
          setState({ status: "success", results });
        }
      },
      (error: unknown) => {
        if (!controller.signal.aborted) {
          const message = error instanceof Error ? error.message : "Search failed";
          setState({ status: "error", message });
        }
      },
    );
    return () => controller.abort();
  }, [debounced]);

  return state;
}
