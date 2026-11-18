// Helpers shared by the contract tests of every stage: a fake MovieApi whose answers the test
// controls one by one, and readers that see the page only through roles and visible text.
import { act, render, screen, within } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { MovieSearch } from "../MovieSearch.tsx";
import type { Movie, MovieApi, MoviePage, MovieQuery } from "../movies.ts";

export type User = ReturnType<typeof userEvent.setup>;

export type Call = {
  query: MovieQuery;
  signal: AbortSignal;
  /** Answers this request (even when it was aborted) and lets React render the result. */
  answer(page: MoviePage): Promise<void>;
  fail(): Promise<void>;
};

export function fakeApi(): { api: MovieApi; calls: Call[] } {
  const calls: Call[] = [];
  const api: MovieApi = {
    search(query, signal) {
      return new Promise<MoviePage>((resolve, reject) => {
        calls.push({
          query: { ...query },
          signal,
          answer: (page) => act(async () => resolve(page)),
          fail: () => act(async () => reject(new Error("HTTP 500"))),
        });
      });
    },
  };
  return { api, calls };
}

export function movie(n: number, extra: Partial<Movie> = {}): Movie {
  return { id: `m${n}`, title: `Movie ${n}`, year: 2000 + n, genres: ["Drama"], rating: 7, ...extra };
}

/** A page of the given movies; `total` and `pageCount` default to this page alone. */
export function page(movies: readonly Movie[], more: Partial<MoviePage> = {}): MoviePage {
  return { movies, total: movies.length, page: 1, pageCount: 1, ...more };
}

export const ALL_MOVIES: MovieQuery = { text: "", genre: null, sort: "relevance", page: 1 };

export function start(api: MovieApi, genres: readonly string[] = []) {
  // Later stages keep the search in the URL: every test starts from a clean one.
  window.history.replaceState(null, "", "/");
  const user = userEvent.setup();
  const view = render(<MovieSearch api={api} genres={genres} />);
  return { user, ...view };
}

export function last(calls: readonly Call[]): Call {
  const call = calls.at(-1);
  if (!call) {
    throw new Error("the page did not call api.search");
  }
  return call;
}

export const statusText = () => (screen.getByRole("status").textContent ?? "").trim();

/** The titles of the movies shown, in order ([] when the list is not shown). */
export function titles(): string[] {
  const list = screen.queryByRole("list", { name: "Movies" });
  if (!list) {
    return [];
  }
  return within(list)
    .queryAllByRole("heading")
    .map((heading) => (heading.textContent ?? "").trim());
}
