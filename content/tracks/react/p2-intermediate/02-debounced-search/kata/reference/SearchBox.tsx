import { useState } from "react";
import { useSearch, type SearchFn } from "./useSearch.ts";

export type SearchBoxProps = {
  search: SearchFn;
  delayMs?: number;
};

/** An input labelled Search and the state of the search below it. */
export function SearchBox({ search, delayMs = 300 }: SearchBoxProps) {
  const [query, setQuery] = useState("");
  const state = useSearch(query, search, delayMs);
  return (
    <div>
      <label>
        Search <input value={query} onChange={(event) => setQuery(event.target.value)} />
      </label>
      {state.status === "loading" && <p role="status">Searching…</p>}
      {state.status === "error" && <p role="alert">{state.message}</p>}
      {state.status === "success" &&
        (state.results.length === 0 ? (
          <p>No results</p>
        ) : (
          <ul aria-label="Results">
            {state.results.map((result) => (
              <li key={result}>{result}</li>
            ))}
          </ul>
        ))}
    </div>
  );
}
