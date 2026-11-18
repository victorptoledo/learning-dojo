import { useEffect, useId, useRef, useState, type FormEvent, type ReactNode } from "react";
import type { Movie, MovieApi, MoviePage, Sort } from "./movies.ts";

export type MovieSearchProps = {
  api: MovieApi;
  /** The genres the Genre select offers, in this order. */
  genres?: readonly string[];
};

type Search = { text: string; genre: string | null; sort: Sort };
type Phase = "loading" | "ready" | "error";
type More = "idle" | "loading" | "error";

const SORTS: { value: Sort; label: string }[] = [
  { value: "relevance", label: "Relevance" },
  { value: "year", label: "Newest" },
  { value: "rating", label: "Top rated" },
];
const ALL = "";
const FIRST: Search = { text: "", genre: null, sort: "relevance" };

const same = (a: Search, b: Search) => a.text === b.text && a.genre === b.genre && a.sort === b.sort;

function status(phase: Phase, more: More, shown: number, total: number): string {
  if (phase === "loading") {
    return "Loading movies";
  }
  if (phase === "error") {
    return "";
  }
  if (more === "loading") {
    return "Loading more movies";
  }
  if (total === 0) {
    return "No movies found";
  }
  return `Showing ${shown} of ${total} ${total === 1 ? "movie" : "movies"}`;
}

/** A searchable, filterable list of movies that loads page by page. */
export function MovieSearch({ api, genres = [] }: MovieSearchProps): ReactNode {
  const [search, setSearch] = useState<Search>(FIRST);
  const [draft, setDraft] = useState("");
  const [movies, setMovies] = useState<readonly Movie[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageCount, setPageCount] = useState(0);
  const [phase, setPhase] = useState<Phase>("loading");
  const [more, setMore] = useState<More>("idle");
  const controller = useRef<AbortController | null>(null);
  const latest = useRef(0);
  const focusNext = useRef<string | null>(null);
  const searchId = useId();

  function apply(result: MoviePage, append: boolean) {
    setTotal(result.total);
    setPage(result.page);
    setPageCount(result.pageCount);
    setPhase("ready");
    setMore("idle");
    if (!append) {
      setMovies(result.movies);
      return;
    }
    setMovies((shown) => {
      const ids = new Set(shown.map((movie) => movie.id));
      const added = result.movies.filter((movie) => !ids.has(movie.id));
      focusNext.current = added[0]?.id ?? null;
      return [...shown, ...added];
    });
  }

  function load(target: Search, number: number, append: boolean) {
    controller.current?.abort();
    const request = ++latest.current;
    const abort = new AbortController();
    controller.current = abort;
    if (append) {
      setMore("loading");
    } else {
      setPhase("loading");
      setMore("idle");
    }
    api.search({ ...target, page: number }, abort.signal).then(
      (result) => {
        if (request === latest.current) {
          apply(result, append);
        }
      },
      () => {
        if (request !== latest.current) {
          return;
        }
        if (append) {
          setMore("error");
        } else {
          setPhase("error");
          setMovies([]);
        }
      },
    );
  }

  function change(next: Search) {
    setSearch(next);
    load(next, 1, false);
  }

  useEffect(() => {
    load(FIRST, 1, false);
    return () => {
      controller.current?.abort();
      latest.current += 1;
    };
    // Runs once: later searches come from the events below.
  }, []);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const next = { ...search, text: draft.trim() };
    if (!same(next, search) || phase === "error") {
      change(next);
    }
  }

  const shown = phase === "ready" || (phase === "loading" && movies.length > 0);

  return (
    <main>
      <h1>Movies</h1>
      <form role="search" onSubmit={submit}>
        <label htmlFor={`${searchId}-text`}>Search movies</label>
        <input
          id={`${searchId}-text`}
          type="search"
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
        />
        <button type="submit">Search</button>
        <label htmlFor={`${searchId}-genre`}>Genre</label>
        <select
          id={`${searchId}-genre`}
          value={search.genre ?? ALL}
          onChange={(event) => change({ ...search, genre: event.target.value === ALL ? null : event.target.value })}
        >
          <option value={ALL}>All genres</option>
          {genres.map((genre) => (
            <option key={genre} value={genre}>
              {genre}
            </option>
          ))}
        </select>
        <label htmlFor={`${searchId}-sort`}>Sort by</label>
        <select
          id={`${searchId}-sort`}
          value={search.sort}
          onChange={(event) => change({ ...search, sort: event.target.value as Sort })}
        >
          {SORTS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </form>

      <p role="status">{status(phase, more, movies.length, total)}</p>

      {phase === "error" ? (
        <div role="alert">
          <p>Could not load movies.</p>
          <button type="button" onClick={() => load(search, 1, false)}>
            Try again
          </button>
        </div>
      ) : null}

      {shown && movies.length > 0 ? (
        <ul aria-label="Movies" aria-busy={phase === "loading" ? true : undefined}>
          {movies.map((movie) => (
            <li key={movie.id}>
              <h3
                tabIndex={-1}
                ref={(element) => {
                  if (element && focusNext.current === movie.id) {
                    focusNext.current = null;
                    element.focus();
                  }
                }}
              >
                {movie.title}
              </h3>
              <p>{`${movie.year} · ${movie.genres.join(", ")} · ★ ${movie.rating.toFixed(1)}`}</p>
            </li>
          ))}
        </ul>
      ) : null}

      {more === "error" ? (
        <div role="alert">
          <p>Could not load more movies.</p>
          <button type="button" onClick={() => load(search, page + 1, true)}>
            Try again
          </button>
        </div>
      ) : null}

      {phase === "ready" && more !== "error" && page < pageCount ? (
        <button type="button" disabled={more === "loading"} onClick={() => load(search, page + 1, true)}>
          Load more
        </button>
      ) : null}
    </main>
  );
}
