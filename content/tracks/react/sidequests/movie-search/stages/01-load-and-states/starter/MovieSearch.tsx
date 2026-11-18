import type { ReactNode } from "react";
import type { MovieApi } from "./movies.ts";

export type MovieSearchProps = {
  api: MovieApi;
  /** The genres the Genre select offers, in this order (stage 2). */
  genres?: readonly string[];
};

/** The movie list: loads, shows its states and, stage after stage, searches, pages and caches. */
export function MovieSearch(_props: MovieSearchProps): ReactNode {
  throw new Error("todo: write MovieSearch");
}
