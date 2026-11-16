import type { ReactNode } from "react";
import type { SearchFn } from "./useSearch.ts";

export type SearchBoxProps = {
  search: SearchFn;
  delayMs?: number;
};

/** An input labelled Search and the state of the search below it. */
export function SearchBox(_props: SearchBoxProps): ReactNode {
  throw new Error("todo: write SearchBox");
}
