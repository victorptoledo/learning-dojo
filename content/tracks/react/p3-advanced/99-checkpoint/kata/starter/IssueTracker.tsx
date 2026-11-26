import type { ReactNode } from "react";
import type { IssueState } from "./issues.ts";
import type { Store } from "./store.ts";

export type IssueTrackerProps = {
  store: Store<IssueState>;
};

/** The filter, the list of issues and the editor of the selected issue (see README.md). */
export function IssueTracker(_props: IssueTrackerProps): ReactNode {
  throw new Error("todo: write IssueTracker");
}
