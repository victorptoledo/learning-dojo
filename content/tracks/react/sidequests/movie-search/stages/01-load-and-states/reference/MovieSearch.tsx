import { useEffect, useState, type ReactNode } from "react";
import type { Movie, MovieApi, MovieQuery } from "./movies.ts";

export type MovieSearchProps = {
  api: MovieApi;
  /** The genres the Genre select offers, in this order. */
  genres?: readonly string[];
};

type State =
  | { phase: "loading" }
  | { phase: "ready"; movies: readonly Movie[]; total: number }
  | { phase: "error" };

const FIRST: MovieQuery = { text: "", genre: null, sort: "relevance", page: 1 };

function status(state: State): string {
  switch (state.phase) {
    case "loading":
      return "Loading movies";
    case "error":
      return "";
    case "ready":
      if (state.total === 0) {
        return "No movies found";
      }
      return `Showing ${state.movies.length} of ${state.total} ${state.total === 1 ? "movie" : "movies"}`;
  }
}

/** The first page of movies, with its loading, error and empty states. */
export function MovieSearch({ api }: MovieSearchProps): ReactNode {
  const [state, setState] = useState<State>({ phase: "loading" });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const abort = new AbortController();
    let current = true;
    setState({ phase: "loading" });
    api.search(FIRST, abort.signal).then(
      (result) => {
        if (current) {
          setState({ phase: "ready", movies: result.movies, total: result.total });
        }
      },
      () => {
        if (current) {
          setState({ phase: "error" });
        }
      },
    );
    return () => {
      current = false;
      abort.abort();
    };
  }, [api, attempt]);

  return (
    <main>
      <h1>Movies</h1>
      <p role="status">{status(state)}</p>
      {state.phase === "error" ? (
        <div role="alert">
          <p>Could not load movies.</p>
          <button type="button" onClick={() => setAttempt(attempt + 1)}>
            Try again
          </button>
        </div>
      ) : null}
      {state.phase === "ready" && state.movies.length > 0 ? (
        <ul aria-label="Movies">
          {state.movies.map((movie) => (
            <li key={movie.id}>
              <h3>{movie.title}</h3>
              <p>{`${movie.year} · ${movie.genres.join(", ")} · ★ ${movie.rating.toFixed(1)}`}</p>
            </li>
          ))}
        </ul>
      ) : null}
    </main>
  );
}
