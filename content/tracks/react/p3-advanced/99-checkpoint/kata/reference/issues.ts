export type Issue = {
  readonly id: string;
  readonly title: string;
  readonly done: boolean;
};

export type IssueState = {
  readonly issues: readonly Issue[];
};

/** Flips `done` of issue `id`; the same state when there is no such issue. */
export function toggleIssue(state: IssueState, id: string): IssueState {
  if (!state.issues.some((issue) => issue.id === id)) {
    return state;
  }
  return {
    ...state,
    issues: state.issues.map((issue) => (issue.id === id ? { ...issue, done: !issue.done } : issue)),
  };
}

/** Renames issue `id` to the trimmed title; the same state when nothing would change. */
export function renameIssue(state: IssueState, id: string, title: string): IssueState {
  const trimmed = title.trim();
  const current = state.issues.find((issue) => issue.id === id);
  if (!current || trimmed === "" || current.title === trimmed) {
    return state;
  }
  return {
    ...state,
    issues: state.issues.map((issue) => (issue.id === id ? { ...issue, title: trimmed } : issue)),
  };
}

/** True when the title contains the trimmed query, ignoring case. */
export function matchesQuery(issue: Issue, query: string): boolean {
  return issue.title.toLowerCase().includes(query.trim().toLowerCase());
}

/** How many issues are not done. */
export function openCount(issues: readonly Issue[]): number {
  return issues.filter((issue) => !issue.done).length;
}
