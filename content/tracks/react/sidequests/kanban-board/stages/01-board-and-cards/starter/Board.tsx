import type { ReactNode } from "react";
import type { BoardApi, BoardData } from "./board-types.ts";

export type BoardProviderProps = {
  initial: BoardData;
  /** Makes the id of a new card. */
  createId?: () => string;
  /** Saves every change (stage 4). */
  api?: BoardApi;
  children: ReactNode;
};

/** Holds the board for every component under it. */
export function BoardProvider(_props: BoardProviderProps): ReactNode {
  throw new Error("todo: write BoardProvider");
}

/** The columns and their cards. */
export function Board(): ReactNode {
  throw new Error("todo: write Board");
}

/** How many cards each column has; it can be rendered anywhere under the provider. */
export function BoardSummary(): ReactNode {
  throw new Error("todo: write BoardSummary");
}
