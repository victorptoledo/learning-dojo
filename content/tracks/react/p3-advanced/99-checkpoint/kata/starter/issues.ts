export type Issue = {
  readonly id: string;
  readonly title: string;
  readonly done: boolean;
};

export type IssueState = {
  readonly issues: readonly Issue[];
};

/** Flips `done` of issue `id`; the same state when there is no such issue. */
export function toggleIssue(_state: IssueState, _id: string): IssueState {
  throw new Error("todo: write toggleIssue");
}

/** Renames issue `id` to the trimmed title; the same state when nothing would change. */
export function renameIssue(_state: IssueState, _id: string, _title: string): IssueState {
  throw new Error("todo: write renameIssue");
}

/** True when the title contains the trimmed query, ignoring case. */
export function matchesQuery(_issue: Issue, _query: string): boolean {
  throw new Error("todo: write matchesQuery");
}

/** How many issues are not done. */
export function openCount(_issues: readonly Issue[]): number {
  throw new Error("todo: write openCount");
}
