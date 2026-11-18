// The contract between the page and the movie API. The tests pass their own `MovieApi`, so
// keep these types as they are; everything else is yours.

export type Movie = {
  id: string;
  title: string;
  year: number;
  genres: readonly string[];
  /** From 0 to 10. */
  rating: number;
};

export type Sort = "relevance" | "year" | "rating";

export type MovieQuery = {
  /** The search text, trimmed; "" lists every movie. */
  text: string;
  /** null for every genre. */
  genre: string | null;
  sort: Sort;
  /** 1 for the first page. */
  page: number;
};

export type MoviePage = {
  movies: readonly Movie[];
  /** How many movies match the query, on every page. */
  total: number;
  page: number;
  pageCount: number;
};

export type MovieApi = {
  /** Rejects when the request fails. Aborting `signal` should cancel it, but an API may answer anyway. */
  search(query: MovieQuery, signal: AbortSignal): Promise<MoviePage>;
};
