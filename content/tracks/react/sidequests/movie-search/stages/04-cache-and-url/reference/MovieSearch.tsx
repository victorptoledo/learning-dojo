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

const keyOf = (search: Search, page: number) => JSON.stringify([search.text, search.genre, search.sort, page]);
const same = (a: Search, b: Search) => a.text === b.text && a.genre === b.genre && a.sort === b.sort;

function readUrl(genres: readonly string[]): Search {
  const params = new URLSearchParams(window.location.search);
  const genre = params.get("genre");
  const sort = params.get("sort");
  return {
    text: (params.get("q") ?? "").trim(),
    genre: genre !== null && genres.includes(genre) ? genre : null,
    sort: SORTS.some((option) => option.value === sort) ? (sort as Sort) : "relevance",
  };
}

function urlOf(search: Search): string {
  const params = new URLSearchParams();
  if (search.text !== "") {
    params.set("q", search.text);
  }
  if (search.genre !== null) {
    params.set("genre", search.genre);
  }
  if (search.sort !== "relevance") {
    params.set("sort", search.sort);
  }
  const query = params.toString();
  return query === "" ? window.location.pathname : `${window.location.pathname}?${query}`;
}

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
  const [search, setSearch] = useState<Search>(() => readUrl(genres));
  const [draft, setDraft] = useState(search.text);
  const [movies, setMovies] = useState<readonly Movie[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageCount, setPageCount] = useState(0);
  const [phase, setPhase] = useState<Phase>("loading");
  const [more, setMore] = useState<More>("idle");
  const controller = useRef<AbortController | null>(null);
  const latest = useRef(0);
  const cache = useRef(new Map<string, MoviePage>());
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
    controller.current = null;
    const request = ++latest.current;
    const cached = cache.current.get(keyOf(target, number));
    if (cached) {
      apply(cached, append);
      return;
    }
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
        if (request !== latest.current) {
          return;
        }
        cache.current.set(keyOf(target, number), result);
        apply(result, append);
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
    if (urlOf(next) !== window.location.pathname + window.location.search) {
      window.history.pushState(null, "", urlOf(next));
    }
    setSearch(next);
    load(next, 1, false);
  }

  useEffect(() => {
    load(search, 1, false);
    function onPopState() {
      const next = readUrl(genres);
      setSearch(next);
      setDraft(next.text);
      load(next, 1, false);
    }
    window.addEventListener("popstate", onPopState);
    return () => {
      window.removeEventListener("popstate", onPopState);
      controller.current?.abort();
      latest.current += 1;
    };
    // Runs once: the first search comes from the URL, later ones from the events above.
  }, []);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const next = { ...search, text: draft.trim() };
    if (!same(next, search) || phase === "error") {
      change(next);
    }
  }

  function refresh() {
    for (const key of [...cache.current.keys()]) {
      const [text, genre, sort] = JSON.parse(key) as [string, string | null, Sort, number];
      if (same({ text, genre, sort }, search)) {
        cache.current.delete(key);
      }
    }
    load(search, 1, false);
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
        <button type="button" onClick={refresh}>
          Refresh
        </button>
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
